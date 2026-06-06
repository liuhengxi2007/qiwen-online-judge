package domains.judge.utils

import cats.syntax.all.*
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.submission.objects.SubmissionResultDisplayMode
import domains.submission.objects.internal.{ClaimedSubmission, SubmissionProgramManifest}
import judgeprotocol.objects.{ProblemSlug, SubmissionId, SubmissionLanguage, SubmissionSourceCode, TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import judgeprotocol.objects.response.*
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import scala.jdk.CollectionConverters.*
import scala.util.Try

object JudgeTaskBuilder:

  final case class ReadyValidation(
    retainedPaths: Set[ProblemDataPath],
    resultDisplayMode: SubmissionResultDisplayMode
  )

  final case class GeneratedHackTestcase(
    subtaskIndex: Int,
    label: Option[String],
    input: JudgeTaskFileRef,
    answer: JudgeTaskFileRef
  )

  final case class BuildError(
    message: String,
    reason: JudgeFailureReason
  )

  private val RolePattern = "^[A-Za-z0-9_-]+$".r

  private final case class StandardConfig(language: SubmissionLanguage, path: String)

  def buildJudgeTask(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    hackTestcases: List[GeneratedHackTestcase] = Nil
  ): Either[BuildError, JudgeTask] =
    parseConfigBytesDetailed(bytes, claimedSubmission, sourceCodes, manifest, hackTestcases)

  private def toProtocolLanguage(language: domains.submission.objects.SubmissionLanguage): SubmissionLanguage =
    language match
      case domains.submission.objects.SubmissionLanguage.Cpp17 => SubmissionLanguage.Cpp17
      case domains.submission.objects.SubmissionLanguage.Python3 => SubmissionLanguage.Python3

  private[utils] def parseConfigBytes(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCode: domains.submission.objects.SubmissionSourceCode,
    manifest: ProblemDataManifest
  ): Either[String, JudgeTask] =
    parseConfigBytes(bytes, claimedSubmission, Map(SubmissionProgramManifest.DefaultProgramKey -> sourceCode), manifest, Nil)

  private[utils] def parseConfigBytes(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    hackTestcases: List[GeneratedHackTestcase] = Nil
  ): Either[String, JudgeTask] =
    parseConfigBytesDetailed(bytes, claimedSubmission, sourceCodes, manifest, hackTestcases).left.map(_.message)

  private def parseConfigBytesDetailed(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    hackTestcases: List[GeneratedHackTestcase]
  ): Either[BuildError, JudgeTask] =
    parseYaml(bytes).flatMap { root =>
      buildFromYaml(claimedSubmission, sourceCodes, manifest, root, hackTestcases)
    }

  def validateReadyConfigBytes(
    bytes: Array[Byte],
    problem: domains.problem.objects.response.ProblemDetail,
    manifest: ProblemDataManifest
  ): Either[String, ReadyValidation] =
    val sourceCode = domains.submission.objects.SubmissionSourceCode("int main() { return 0; }")
    val claimedSubmission = ClaimedSubmission(
      id = domains.submission.objects.SubmissionId(0L),
      problemId = problem.id,
      problemSlug = problem.slug,
      programManifest = SubmissionProgramManifest.singleDefault(
        java.util.UUID.fromString("00000000-0000-4000-8000-000000000000"),
        domains.submission.objects.SubmissionLanguage.Cpp17,
        sourceCode
      )
    )
    parseConfigBytes(bytes, claimedSubmission, sourceCode, manifest).flatMap { task =>
      val rawPaths =
        task.subtasks.flatMap { subtask =>
          val subtaskPaths =
            subtask.validator.map(_.source.path.value).toList ++
              subtask.standard.map(_.source.path.value).toList ++
              subtask.mode.interactor.map(_.source.path.value).toList
          val testcasePaths = subtask.testcases.flatMap { testcase =>
            List(
              Some(testcase.input.path.value),
              testcase.answer.map(_.path.value),
              testcase.checker.source.map(_.path.value),
              testcase.strategyProvider.map(_.source.path.value)
            ).flatten
          }
          subtaskPaths ++ testcasePaths
        }
      rawPaths
        .traverse(ProblemDataPath.parse)
        .map(paths => ReadyValidation(paths.toSet + ProblemDataPath("judge.yaml"), resultDisplayModeFor(task)))
    }

  private[utils] def resultDisplayModeFor(task: JudgeTask): SubmissionResultDisplayMode =
    task.subtasks match
      case singleSubtask :: Nil
          if singleSubtask.aggregation.score == "min" &&
            (singleSubtask.aggregation.time == "max" || singleSubtask.aggregation.time == "sum") &&
            singleSubtask.aggregation.memory == "max" =>
        SubmissionResultDisplayMode.Verdict
      case _ =>
        SubmissionResultDisplayMode.Score

  private def parseYaml(bytes: Array[Byte]): Either[BuildError, Map[String, Any]] =
    Try {
        val settings = LoadSettings.builder().setLabel("judge.yaml").build()
        val loaded = Load(settings).loadFromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
        toScalaMap(loaded)
      }
      .toEither
      .left
      .map(error => BuildError(s"Invalid judge.yaml: ${error.getMessage}", JudgeFailureReason.JudgeTaskBuildFailed))

  private def buildFromYaml(
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    root: Map[String, Any],
    hackTestcases: List[GeneratedHackTestcase]
  ): Either[BuildError, JudgeTask] =
    for
      version <- optionalIntAt(root, "version").toBuildError.flatMap {
        case Some(2) => Right(2)
        case _ => Left(buildError("judge.yaml version must be 2."))
      }
      _ = version
      roundingScale <- optionalIntAt(root, "roundingScale").toBuildError.map(_.getOrElse(6))
      _ <- Either.cond(roundingScale >= 0 && roundingScale <= 18, (), buildError("roundingScale must be between 0 and 18."))
      rootLimits <- limitsAt(root).toBuildError
      rootChecker <- checkerAt(root).toBuildError
      rootValidator <- toolAt(root, "validator").toBuildError
      rootStandard <- standardAt(root).toBuildError
      rootMode <- modeAt(root, manifest).toBuildError.map(_.getOrElse(JudgeTaskMode.traditional(SubmissionProgramManifest.DefaultProgramKey)))
      rootStrategyProvider <- limitedToolAt(root, "strategyProvider").toBuildError
      rootAggregation <- aggregationAt(root).toBuildError
      subtaskMaps <- listOfMapsAt(root, "subtasks").toBuildError
      _ <- Either.cond(subtaskMaps.nonEmpty, (), buildError("judge.yaml must define at least one subtask."))
      subtaskRatios <- ratiosFor(subtaskMaps).toBuildError
      subtasks <- sequenceBuild(subtaskMaps.zip(subtaskRatios).zipWithIndex.map { case ((subtaskMap, subtaskRatio), subtaskIndex) =>
        buildSubtask(
          manifest = manifest,
          raw = subtaskMap,
          index = subtaskIndex + 1,
          scoreRatio = subtaskRatio,
          parentLimits = rootLimits,
          parentChecker = rootChecker,
          parentValidator = rootValidator,
          parentStandard = rootStandard,
          parentMode = rootMode,
          parentStrategyProvider = rootStrategyProvider,
          parentAggregation = rootAggregation,
          hackTestcases = hackTestcases.filter(_.subtaskIndex == subtaskIndex + 1)
        )
      })
      programs <- buildPrograms(claimedSubmission, sourceCodes)
      taskAggregation = rootAggregation.subtasks.getOrElse(defaultAggregation)
    yield
      JudgeTask(
        submissionId = SubmissionId(claimedSubmission.id.value),
        problemSlug = ProblemSlug(claimedSubmission.problemSlug.value),
        programs = programs,
        problemDataVersion = manifest.version,
        roundingScale = roundingScale,
        aggregation = taskAggregation,
        subtasks = subtasks
      )

  private def buildPrograms(
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode]
  ): Either[BuildError, Map[String, JudgeTaskProgram]] =
    sequenceBuild(claimedSubmission.programManifest.programs.toList.map { case (role, program) =>
      sourceCodes
        .get(role)
        .toRight(buildError(s"Source code for submission role $role was not found."))
        .map(sourceCode => role -> JudgeTaskProgram(toProtocolLanguage(program.language), SubmissionSourceCode(sourceCode.value)))
    }).map(_.toMap)

  private def buildSubtask(
    manifest: ProblemDataManifest,
    raw: Map[String, Any],
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[JudgeTaskChecker],
    parentValidator: Option[JudgeTaskTool],
    parentStandard: Option[StandardConfig],
    parentMode: JudgeTaskMode,
    parentStrategyProvider: Option[JudgeTaskTool],
    parentAggregation: AggregationConfig,
    hackTestcases: List[GeneratedHackTestcase]
  ): Either[BuildError, JudgeTaskSubtask] =
    for
      label <- optionalStringAt(raw, "label").toBuildError
      subtaskLabel = judgeNodeLabel("subtask", index, label)
      _ <- rejectLegacyName(raw, subtaskLabel)
      limits <- limitsAt(raw).toBuildError.map(_.orElse(parentLimits))
      checker <- checkerAt(raw).toBuildError.map(_.orElse(parentChecker))
      validator <- toolAt(raw, "validator").toBuildError.map(_.orElse(parentValidator))
      resolvedValidator <- validator.traverse(resolveTool(manifest, _, "Validator source file")).toBuildError
      standard <- standardAt(raw).toBuildError.map(_.orElse(parentStandard))
      resolvedStandard <- standard.traverse(resolveStandard(manifest, _)).toBuildError
      mode <- modeAt(raw, manifest).toBuildError.map(_.getOrElse(parentMode))
      strategyProvider <- limitedToolAt(raw, "strategyProvider").toBuildError.map(_.orElse(parentStrategyProvider))
      aggregation <- aggregationAt(raw).toBuildError.map(parentAggregation.merge)
      testcaseMaps <- listOfMapsAt(raw, "testcases").toBuildError
      _ <- Either.cond(testcaseMaps.nonEmpty, (), buildError(s"$subtaskLabel must define at least one testcase."))
      testcaseTypes <- sequence(testcaseMaps.map(testcaseTypeAt)).toBuildError
      testcaseLabels <- sequenceBuild(testcaseMaps.zipWithIndex.map { case (testcaseMap, testcaseIndex) =>
        optionalStringAt(testcaseMap, "label").toBuildError.map(label => testcaseIndex -> label)
      }).map(_.toMap)
      _ <- sequenceBuild(testcaseMaps.zip(testcaseTypes).zipWithIndex.map { case ((testcaseMap, testcaseType), testcaseIndex) =>
        rejectNonMainTestcaseScoreRatio(testcaseMap, testcaseType, s"$subtaskLabel ${judgeNodeLabel("testcase", testcaseIndex + 1, testcaseLabels.getOrElse(testcaseIndex, None))}")
      })
      mainTestcaseMaps = testcaseMaps.zip(testcaseTypes).collect { case (testcaseMap, JudgeTestcaseType.Main) => testcaseMap }
      _ <- Either.cond(mainTestcaseMaps.nonEmpty, (), buildError(s"$subtaskLabel must define at least one main testcase."))
      mainTestcaseRatios <- ratiosFor(mainTestcaseMaps).toBuildError
      mainRatioByIndex = testcaseMaps.zip(testcaseTypes).zipWithIndex.collect {
        case ((_, JudgeTestcaseType.Main), testcaseIndex) => testcaseIndex
      }.zip(mainTestcaseRatios).toMap
      configuredTestcases <- sequenceBuild(testcaseMaps.zip(testcaseTypes).zipWithIndex.map { case ((testcaseMap, testcaseType), testcaseIndex) =>
        buildTestcase(
          manifest = manifest,
          raw = testcaseMap,
          index = testcaseIndex + 1,
          scoreRatio = if testcaseType == JudgeTestcaseType.Main then mainRatioByIndex(testcaseIndex) else BigDecimal(0),
          parentLimits = limits,
          parentChecker = checker,
          parentStrategyProvider = strategyProvider,
          subtaskIndex = index,
          subtaskLabel = label
        )
      })
      firstMainTestcase = configuredTestcases.find(_.testcaseType == JudgeTestcaseType.Main).getOrElse(configuredTestcases.head)
      generatedHackTestcases = hackTestcases.zipWithIndex.map { case (testcase, offset) =>
        JudgeTaskTestcase(
          index = configuredTestcases.size + offset + 1,
          label = testcase.label,
          testcaseType = JudgeTestcaseType.Hack,
          scoreRatio = BigDecimal(0),
          limits = firstMainTestcase.limits,
          checker = firstMainTestcase.checker,
          input = testcase.input,
          answer = Some(testcase.answer),
          strategyProvider = firstMainTestcase.strategyProvider
        )
      }
      testcases = configuredTestcases ++ generatedHackTestcases
    yield
      JudgeTaskSubtask(
        index = index,
        label = label,
        scoreRatio = scoreRatio,
        mode = mode,
        validator = resolvedValidator,
        standard = resolvedStandard,
        aggregation = aggregation.testcases.getOrElse(defaultAggregation),
        testcases = testcases
      )

  private def buildTestcase(
    manifest: ProblemDataManifest,
    raw: Map[String, Any],
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[JudgeTaskChecker],
    parentStrategyProvider: Option[JudgeTaskTool],
    subtaskIndex: Int,
    subtaskLabel: Option[String]
  ): Either[BuildError, JudgeTaskTestcase] =
    for
      testcaseLabel <- optionalStringAt(raw, "label").toBuildError
      testcaseType <- testcaseTypeAt(raw).toBuildError
      label = s"${judgeNodeLabel("subtask", subtaskIndex, subtaskLabel)} ${judgeNodeLabel("testcase", index, testcaseLabel)}"
      _ <- rejectLegacyName(raw, label)
      _ <- Either.cond(!raw.contains("mode"), (), buildError(s"mode cannot be declared on $label."))
      _ <- Either.cond(!raw.contains("validator"), (), buildError(s"validator cannot be declared on $label."))
      limits <- limitsAt(raw).toBuildError.map(_.orElse(parentLimits)).flatMap(_.toRight(buildError(s"Limits are required for $label.")))
      checker <- checkerAt(raw).toBuildError.map(_.orElse(parentChecker)).flatMap(_.toRight(buildError(s"Checker is required for $label.")))
      strategyProvider <- limitedToolAt(raw, "strategyProvider").toBuildError.map(_.orElse(parentStrategyProvider))
      resolvedChecker <- resolveChecker(manifest, checker).toBuildError
      resolvedStrategyProvider <- strategyProvider.traverse(resolveTool(manifest, _, "Strategy provider source file")).toBuildError
      inputPath <- stringAt(raw, "input").toBuildError
      answerPath <- optionalStringAt(raw, "answer").toBuildError
      inputRef <- findFile(manifest, inputPath, s"Input file for $label").toBuildError
      answerRef <- answerPath.traverse(path => findFile(manifest, path, s"Answer file for $label")).toBuildError
      _ <- Either.cond(
        resolvedChecker.`type` != "builtin" || !resolvedChecker.name.contains("exact") || answerRef.nonEmpty,
        (),
        buildError(s"Answer file is required for $label when using builtin exact checker.")
      )
    yield
      JudgeTaskTestcase(
        index = index,
        label = testcaseLabel,
        testcaseType = testcaseType,
        scoreRatio = scoreRatio,
        limits = limits,
        checker = resolvedChecker,
        input = inputRef,
        answer = answerRef,
        strategyProvider = resolvedStrategyProvider
      )

  private def testcaseTypeAt(raw: Map[String, Any]): Either[String, JudgeTestcaseType] =
    optionalStringAt(raw, "type").flatMap {
      case None => Right(JudgeTestcaseType.Main)
      case Some(value) => JudgeTestcaseType.parse(value)
    }

  private def rejectNonMainTestcaseScoreRatio(
    raw: Map[String, Any],
    testcaseType: JudgeTestcaseType,
    label: String
  ): Either[BuildError, Unit] =
    Either.cond(
      testcaseType == JudgeTestcaseType.Main || !raw.contains("scoreRatio"),
      (),
      buildError(s"scoreRatio cannot be declared on $label when type is ${JudgeTestcaseType.render(testcaseType)}.")
    )

  private def standardAt(raw: Map[String, Any]): Either[String, Option[StandardConfig]] =
    optionalMapAt(raw, "standard").flatMap {
      case None => Right(None)
      case Some(standard) =>
        for
          language <- stringAt(standard, "language").flatMap(parseStandardLanguage)
          path <- stringAt(standard, "path")
          _ <- ProblemDataPath.parse(path).left.map(message => s"Invalid standard path: $message")
        yield Some(StandardConfig(language, path))
    }

  private def parseStandardLanguage(raw: String): Either[String, SubmissionLanguage] =
    raw.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case other => Left(s"Unsupported standard language: $other.")

  private def modeAt(raw: Map[String, Any], manifest: ProblemDataManifest): Either[String, Option[JudgeTaskMode]] =
    raw.get("mode") match
      case None => Right(None)
      case Some(value: String) if value.trim == "traditional" =>
        Right(Some(JudgeTaskMode.traditional(SubmissionProgramManifest.DefaultProgramKey)))
      case Some(value: String) if value.trim == "interactive" =>
        Left("mode must be an object when interactive mode is selected.")
      case Some(mode: Map[?, ?]) =>
        val modeMap = mode.asInstanceOf[Map[String, Any]]
        stringAt(modeMap, "type").flatMap {
          case "traditional" =>
            optionalStringAt(modeMap, "role").flatMap(_.getOrElse(SubmissionProgramManifest.DefaultProgramKey).validateRole).map(role =>
              Some(JudgeTaskMode.traditional(role))
            )
          case "interactive" =>
            for
              rawRoles <- listOfStringsAt(modeMap, "roles")
              roles <- rawRoles.traverse(_.validateRole)
              _ <- Either.cond(roles.nonEmpty, (), "interactive mode must declare at least one role.")
              interactor <- requiredLimitedToolFrom(modeMap.get("interactor"), "mode.interactor")
              resolvedInteractor <- resolveTool(manifest, interactor, "Interactor source file")
            yield Some(JudgeTaskMode.interactive(roles, resolvedInteractor))
          case other => Left(s"Unsupported judge mode: $other.")
        }
      case Some(_) => Left("mode must be a string or an object.")

  private def checkerAt(raw: Map[String, Any]): Either[String, Option[JudgeTaskChecker]] =
    optionalMapAt(raw, "checker").flatMap {
      case None => Right(None)
      case Some(checker) =>
        stringAt(checker, "type").flatMap {
          case "builtin" =>
            stringAt(checker, "name").flatMap {
              case "exact" => Right(Some(JudgeTaskChecker("builtin", Some("exact"), None)))
              case "echo" => Right(Some(JudgeTaskChecker("builtin", Some("echo"), None)))
              case other => Left(s"Unsupported builtin checker: $other.")
            }
          case "cpp17" | "cpp" =>
            stringAt(checker, "path").flatMap(path =>
              ProblemDataPath.parse(path)
                .left.map(message => s"Invalid checker path: $message")
                .map(_ => Some(JudgeTaskChecker("cpp17", None, Some(JudgeTaskFileRef.unsafe(path, 0L, "0" * 64)))))
            )
          case other => Left(s"Unsupported checker type: $other.")
        }
    }

  private def toolAt(raw: Map[String, Any], key: String): Either[String, Option[JudgeTaskTool]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value) => toolFrom(value, key).map(Some(_))

  private def limitedToolAt(raw: Map[String, Any], key: String): Either[String, Option[JudgeTaskTool]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value) => limitedToolFrom(value, key).map(Some(_))

  private def requiredLimitedToolFrom(value: Option[Any], label: String): Either[String, JudgeTaskTool] =
    value match
      case Some(currentValue) => limitedToolFrom(currentValue, label)
      case None => Left(s"$label is required.")

  private def toolFrom(value: Any, label: String): Either[String, JudgeTaskTool] =
    value match
      case path: String =>
        toolFromPath(path, label)
      case map: Map[?, ?] =>
        toolFromObject(map.asInstanceOf[Map[String, Any]], label, requireLimits = false)
      case _ =>
        Left(s"$label must be a path string or an object with a path.")

  private def limitedToolFrom(value: Any, label: String): Either[String, JudgeTaskTool] =
    value match
      case map: Map[?, ?] =>
        toolFromObject(map.asInstanceOf[Map[String, Any]], label, requireLimits = true)
      case _ =>
        Left(s"$label must be an object with path and limits.")

  private def toolFromObject(raw: Map[String, Any], label: String, requireLimits: Boolean): Either[String, JudgeTaskTool] =
    for
      path <- stringAt(raw, "path")
      tool <- toolFromPath(path, label)
      limits <- toolLimitsAt(raw, label)
      _ <- Either.cond(!requireLimits || limits.nonEmpty, (), s"$label.limits is required.")
    yield tool.copy(limits = limits)

  private def toolFromPath(path: String, label: String): Either[String, JudgeTaskTool] =
    ProblemDataPath.parse(path)
      .left.map(message => s"Invalid $label path: $message")
      .map(_ => JudgeTaskTool(JudgeTaskFileRef.unsafe(path, 0L, "0" * 64), None))

  private def limitsAt(raw: Map[String, Any]): Either[String, Option[JudgeTaskLimits]] =
    optionalMapAt(raw, "limits").flatMap {
      case None => Right(None)
      case Some(limits) =>
        for
          timeMs <- intAt(limits, "timeMs").flatMap(TestcaseTimeLimitMs.parse)
          memoryMb <- intAt(limits, "memoryMb").flatMap(TestcaseMemoryLimitMb.parse)
        yield Some(JudgeTaskLimits(timeMs, memoryMb))
    }

  private def toolLimitsAt(raw: Map[String, Any], label: String): Either[String, Option[JudgeTaskToolLimits]] =
    optionalMapAt(raw, "limits").left.map(message => s"$label.$message").flatMap {
      case None => Right(None)
      case Some(limits) =>
        for
          timeMs <- intAt(limits, "timeMs")
            .left.map(message => s"$label.limits.$message")
            .flatMap(TestcaseTimeLimitMs.parse)
          memoryMb <- intAt(limits, "memoryMb")
            .left.map(message => s"$label.limits.$message")
            .flatMap(TestcaseMemoryLimitMb.parse)
        yield Some(JudgeTaskToolLimits(timeMs, memoryMb))
    }

  private final case class AggregationConfig(testcases: Option[JudgeTaskAggregation], subtasks: Option[JudgeTaskAggregation]):
    def merge(child: AggregationConfig): AggregationConfig =
      AggregationConfig(testcases = child.testcases.orElse(testcases), subtasks = child.subtasks.orElse(subtasks))

  private def aggregationAt(raw: Map[String, Any]): Either[String, AggregationConfig] =
    optionalMapAt(raw, "aggregation").flatMap {
      case None => Right(AggregationConfig(None, None))
      case Some(aggregation) =>
        for
          testcases <- optionalStringAt(aggregation, "testcases").flatMap(_.map(parseAggregation).sequence)
          subtasks <- optionalStringAt(aggregation, "subtasks").flatMap(_.map(parseAggregation).sequence)
        yield AggregationConfig(testcases, subtasks)
    }

  private val allowedAggregations = List(
    "min_max_max" -> JudgeTaskAggregation("min", "max", "max"),
    "min_sum_max" -> JudgeTaskAggregation("min", "sum", "max"),
    "sum_max_max" -> JudgeTaskAggregation("sum", "max", "max"),
    "sum_sum_max" -> JudgeTaskAggregation("sum", "sum", "max")
  )
  private val allowedAggregationByName = allowedAggregations.toMap
  private val defaultAggregation = JudgeTaskAggregation("sum", "max", "max")

  private def parseAggregation(raw: String): Either[String, JudgeTaskAggregation] =
    allowedAggregationByName.get(raw.trim).toRight(s"Unsupported aggregation: $raw. Expected one of: ${allowedAggregations.map(_._1).mkString(", ")}.")

  private def findFile(manifest: ProblemDataManifest, rawPath: String, label: String): Either[String, JudgeTaskFileRef] =
    for
      path <- ProblemDataPath.parse(rawPath).left.map(message => s"$label has invalid path: $message")
      entry <- manifest.entries.find(_.path == path).toRight(s"$label does not exist: $rawPath.")
      ref <- fileRef(entry).left.map(message => s"$label has invalid file reference: $message")
    yield ref

  private def resolveChecker(manifest: ProblemDataManifest, checker: JudgeTaskChecker): Either[String, JudgeTaskChecker] =
    checker.`type` match
      case "cpp17" | "cpp" =>
        checker.source match
          case Some(source) => findFile(manifest, source.path.value, "Checker source file").map(ref => checker.copy(`type` = "cpp17", source = Some(ref)))
          case None => Left("C++17 checker source path is required.")
      case _ => Right(checker)

  private def resolveTool(manifest: ProblemDataManifest, tool: JudgeTaskTool, label: String): Either[String, JudgeTaskTool] =
    findFile(manifest, tool.source.path.value, label).map(ref => tool.copy(source = ref))

  private def resolveStandard(manifest: ProblemDataManifest, standard: StandardConfig): Either[String, JudgeTaskStandard] =
    findFile(manifest, standard.path, "Standard source file").map(ref => JudgeTaskStandard(standard.language, ref))

  private def fileRef(entry: ProblemDataManifestEntry): Either[String, JudgeTaskFileRef] =
    JudgeTaskFileRef.from(entry.path.value, entry.sizeBytes, entry.sha256)

  private def ratiosFor(items: List[Map[String, Any]]): Either[String, List[BigDecimal]] =
    val explicit = items.map(optionalDecimalAt(_, "scoreRatio"))
    sequence(explicit).flatMap { ratios =>
      val missingCount = ratios.count(_.isEmpty)
      val explicitSum = ratios.flatten.sum
      if explicitSum > BigDecimal(1) then Left("scoreRatio values among siblings must not sum above 1.")
      else
        val fallback = if missingCount == 0 then BigDecimal(0) else (BigDecimal(1) - explicitSum) / BigDecimal(missingCount)
        Right(ratios.map(_.getOrElse(fallback)))
    }

  private def rejectLegacyName(raw: Map[String, Any], label: String): Either[BuildError, Unit] =
    Either.cond(!raw.contains("name"), (), buildError(s"$label must use label instead of name."))

  private def judgeNodeLabel(kind: String, index: Int, label: Option[String]): String =
    label match
      case Some(value) => s"$kind $index ($value)"
      case None => s"$kind $index"

  private def toScalaMap(value: Any): Map[String, Any] =
    value match
      case map: java.util.Map[?, ?] =>
        map.asScala.toMap.collect { case (key: String, value) => key -> toScalaValue(value) }
      case _ => Map.empty

  private def toScalaValue(value: Any): Any =
    value match
      case map: java.util.Map[?, ?] => toScalaMap(map)
      case list: java.util.List[?] => list.asScala.toList.map(toScalaValue)
      case other => other

  private def optionalMapAt(raw: Map[String, Any], key: String): Either[String, Option[Map[String, Any]]] =
    raw.get(key) match
      case None => Right(None)
      case Some(map: Map[?, ?]) => Right(Some(map.asInstanceOf[Map[String, Any]]))
      case Some(_) => Left(s"$key must be an object.")

  private def listOfMapsAt(raw: Map[String, Any], key: String): Either[String, List[Map[String, Any]]] =
    raw.get(key) match
      case Some(items: List[?]) =>
        sequence(items.zipWithIndex.map {
          case (map: Map[?, ?], _) => Right(map.asInstanceOf[Map[String, Any]])
          case (_, index) => Left(s"$key[$index] must be an object.")
        })
      case Some(_) => Left(s"$key must be a list.")
      case None => Left(s"$key is required.")

  private def listOfStringsAt(raw: Map[String, Any], key: String): Either[String, List[String]] =
    raw.get(key) match
      case Some(items: List[?]) =>
        sequence(items.zipWithIndex.map {
          case (value: String, _) if value.trim.nonEmpty => Right(value.trim)
          case (_: String, index) => Left(s"$key[$index] must not be empty.")
          case (_, index) => Left(s"$key[$index] must be a string.")
        })
      case Some(_) => Left(s"$key must be a list.")
      case None => Left(s"$key is required.")

  private def stringAt(raw: Map[String, Any], key: String): Either[String, String] =
    optionalStringAt(raw, key).flatMap(_.toRight(s"$key is required."))

  private def optionalStringAt(raw: Map[String, Any], key: String): Either[String, Option[String]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value: String) if value.trim.nonEmpty => Right(Some(value.trim))
      case Some(_: String) => Left(s"$key must not be empty.")
      case Some(_) => Left(s"$key must be a string.")

  private def intAt(raw: Map[String, Any], key: String): Either[String, Int] =
    optionalIntAt(raw, key).flatMap(_.toRight(s"$key is required."))

  private def optionalIntAt(raw: Map[String, Any], key: String): Either[String, Option[Int]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value: Integer) => Right(Some(value.intValue))
      case Some(value: java.lang.Long) if value >= Int.MinValue && value <= Int.MaxValue => Right(Some(value.intValue))
      case Some(value: java.math.BigInteger) if value.bitLength < 31 => Right(Some(value.intValue))
      case Some(value) => Left(s"$key must be an integer, found ${value.getClass.getSimpleName}.")

  private def optionalDecimalAt(raw: Map[String, Any], key: String): Either[String, Option[BigDecimal]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value: java.lang.Integer) => validateRatio(BigDecimal(value.intValue), key).map(Some(_))
      case Some(value: java.lang.Long) => validateRatio(BigDecimal(value.longValue), key).map(Some(_))
      case Some(value: java.math.BigInteger) => validateRatio(BigDecimal(value), key).map(Some(_))
      case Some(value: java.math.BigDecimal) => validateRatio(BigDecimal(value), key).map(Some(_))
      case Some(value: java.lang.Double) => validateRatio(BigDecimal(value.doubleValue), key).map(Some(_))
      case Some(value) => Left(s"$key must be a number, found ${value.getClass.getSimpleName}.")

  private def validateRatio(value: BigDecimal, key: String): Either[String, BigDecimal] =
    Either.cond(value >= 0 && value <= 1, value, s"$key must be between 0 and 1.")

  private def sequence[A](items: List[Either[String, A]]): Either[String, List[A]] =
    items.foldRight(Right(Nil): Either[String, List[A]])((item, acc) => item.flatMap(value => acc.map(value :: _)))

  private def sequenceBuild[A](items: List[Either[BuildError, A]]): Either[BuildError, List[A]] =
    items.foldRight(Right(Nil): Either[BuildError, List[A]])((item, acc) => item.flatMap(value => acc.map(value :: _)))

  private def buildError(message: String): BuildError =
    BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed)

  extension [A](either: Either[String, A])
    private def toBuildError: Either[BuildError, A] =
      either.left.map(buildError)

  extension (role: String)
    private def validateRole: Either[String, String] =
      Either.cond(RolePattern.findFirstIn(role).contains(role), role, s"Role must contain only ASCII letters, digits, '_' or '-': $role.")

  extension [A](option: Option[Either[String, A]])
    private def sequence: Either[String, Option[A]] =
      option match
        case Some(value) => value.map(Some(_))
        case None => Right(None)
