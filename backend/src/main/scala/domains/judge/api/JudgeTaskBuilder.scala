package domains.judge.api

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

/** judge.yaml 到 judge-protocol 任务的构建器；负责配置解析、文件引用校验、角色/语言约束和 ready 校验。 */
object JudgeTaskBuilder:

  /** 题目数据 ready 校验结果；retainedPaths 是 ready 后仍需保留的数据文件集合。 */
  final case class ReadyValidation(
    retainedPaths: Set[ProblemDataPath],
    resultDisplayMode: SubmissionResultDisplayMode
  )

  /** 构建判题任务失败的业务错误；reason 会映射到 worker 失败 JudgeResult。 */
  final case class BuildError(
    message: String,
    reason: JudgeFailureReason
  )

  private val CodeRolePattern = "^[A-Za-z0-9_-]+$".r
  private val TextRolePattern = "^[A-Za-z0-9_-]+\\.txt$".r

  private type YamlObject = Map[String, YamlValue]

  private enum YamlValue:
    case Obj(value: YamlObject)
    case Arr(values: List[YamlValue])
    case Str(value: String)
    case Bool(value: Boolean)
    case Integral(value: BigInt)
    case Decimal(value: BigDecimal)
    case NullValue

  private enum StandardConfig:
    case Unspecified
    case NoAnswer
    case Generator(language: SubmissionLanguage, path: ProblemDataPath)

    def inherit(parent: StandardConfig): StandardConfig =
      this match
        case Unspecified => parent
        case _ => this

    def generator: Option[StandardConfig.Generator] =
      this match
        case generator @ Generator(_, _) => Some(generator)
        case _ => None

  private enum CheckerConfig:
    case Builtin(name: String)
    case Cpp17(path: ProblemDataPath)

  private final case class ToolConfig(path: ProblemDataPath, limits: Option[JudgeTaskToolLimits])

  private final case class RoleConfig(stubs: Map[SubmissionLanguage, JudgeTaskFileRef]):
    def restrictsLanguages: Boolean = stubs.nonEmpty

  private final case class BuiltTask(task: JudgeTask, roleConfigs: Map[String, RoleConfig])

  /** 从 judge.yaml、已领取提交、源码和数据清单构建 worker 可执行的 JudgeTask。 */
  def buildJudgeTask(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    startedAtEpochMilli: Long = 0L
  ): Either[BuildError, JudgeTask] =
    parseConfigBytesDetailed(bytes, claimedSubmission, sourceCodes, manifest, startedAtEpochMilli)

  private def toProtocolLanguage(language: domains.submission.objects.SubmissionLanguage): SubmissionLanguage =
    language match
      case domains.submission.objects.SubmissionLanguage.Cpp17 => SubmissionLanguage.Cpp17
      case domains.submission.objects.SubmissionLanguage.Python3 => SubmissionLanguage.Python3
      case domains.submission.objects.SubmissionLanguage.Text => SubmissionLanguage.Text

  private[api] def parseConfigBytes(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCode: domains.submission.objects.SubmissionSourceCode,
    manifest: ProblemDataManifest
  ): Either[String, JudgeTask] =
    parseConfigBytes(bytes, claimedSubmission, Map(SubmissionProgramManifest.DefaultProgramKey -> sourceCode), manifest)

  private[api] def parseConfigBytes(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest
  ): Either[String, JudgeTask] =
    parseConfigBytesDetailed(bytes, claimedSubmission, sourceCodes, manifest, startedAtEpochMilli = 0L).left.map(_.message)

  private def parseConfigBytesDetailed(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    startedAtEpochMilli: Long
  ): Either[BuildError, JudgeTask] =
    parseYaml(bytes).flatMap { root =>
      buildFromYaml(claimedSubmission, sourceCodes, manifest, root, startedAtEpochMilli).map(_.task)
    }

  /** 在创建提交前校验提交程序角色/语言是否满足 judge.yaml 的 roles/stubs 约束。 */
  def validateSubmissionProgramsForConfig(
    bytes: Array[Byte],
    programs: Map[String, domains.submission.objects.SubmissionLanguage],
    manifest: ProblemDataManifest
  ): Either[String, Unit] =
    parseYaml(bytes)
      .flatMap { root =>
        for
          _ <- headerRefsAt(root, manifest).toBuildError
          roleConfigs <- roleConfigsAt(root, manifest).toBuildError
          _ <- validateSubmittedProgramLanguages(programs, roleConfigs)
        yield ()
      }
      .left
      .map(_.message)

  /** 在题目数据设为 ready 前完整构建一次任务，输出应保留的数据路径和结果展示模式。 */
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
      // 注意：这里的固定 UUID 只用于 ready 校验时构造临时程序清单，不会写入存储或暴露给真实提交。
      programManifest = SubmissionProgramManifest.singleDefault(
        java.util.UUID.fromString("00000000-0000-4000-8000-000000000000"),
        domains.submission.objects.SubmissionLanguage.Cpp17,
        sourceCode
      )
    )
    parseYaml(bytes)
      .flatMap(root => buildFromYaml(claimedSubmission, Map(SubmissionProgramManifest.DefaultProgramKey -> sourceCode), manifest, root, startedAtEpochMilli = 0L))
      .flatMap { built =>
        val task = built.task
        val rawPaths =
          built.roleConfigs.values.toList.flatMap(_.stubs.values.map(_.path.value)) ++
            task.programs.values.toList.flatMap(_.stub.map(_.path.value)) ++
            task.programs.values.toList.flatMap(_.headers.map(_.path.value)) ++
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
          .left
          .map(buildError)
          .map(paths => ReadyValidation(paths.toSet + ProblemDataPath("judge.yaml"), resultDisplayModeFor(task)))
      }.left.map(_.message)

  private[api] def resultDisplayModeFor(task: JudgeTask): SubmissionResultDisplayMode =
    task.subtasks match
      case singleSubtask :: Nil
          if singleSubtask.aggregation.score == "min" &&
            (singleSubtask.aggregation.time == "max" || singleSubtask.aggregation.time == "sum") &&
            singleSubtask.aggregation.memory == "max" =>
        SubmissionResultDisplayMode.Verdict
      case _ =>
        SubmissionResultDisplayMode.Score

  private def parseYaml(bytes: Array[Byte]): Either[BuildError, YamlObject] =
    Try {
        val settings = LoadSettings.builder().setLabel("judge.yaml").build()
        Load(settings).loadFromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
      }
      .toEither
      .left
      .map(error => BuildError(s"Invalid judge.yaml: ${error.getMessage}", JudgeFailureReason.JudgeTaskBuildFailed))
      .flatMap(loaded => toYamlObject(loaded, "judge.yaml").left.map(message => BuildError(message, JudgeFailureReason.JudgeTaskBuildFailed)))

  private def buildFromYaml(
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    manifest: ProblemDataManifest,
    root: YamlObject,
    startedAtEpochMilli: Long
  ): Either[BuildError, BuiltTask] =
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
      rootHack <- optionalBooleanAt(root, "hack").toBuildError.map(_.getOrElse(true))
      rootMode <- modeAt(root, manifest).toBuildError.map(_.getOrElse(JudgeTaskMode.traditional(SubmissionProgramManifest.DefaultProgramKey)))
      rootStrategyProvider <- limitedToolAt(root, "strategyProvider").toBuildError
      rootAggregation <- aggregationAt(root).toBuildError
      headers <- headerRefsAt(root, manifest).toBuildError
      roleConfigs <- roleConfigsAt(root, manifest).toBuildError
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
          parentHack = rootHack,
          parentMode = rootMode,
          parentStrategyProvider = rootStrategyProvider,
          parentAggregation = rootAggregation
        )
      })
      programs <- buildPrograms(claimedSubmission, sourceCodes, roleConfigs, headers)
      taskAggregation = rootAggregation.subtasks.getOrElse(defaultAggregation)
    yield
      BuiltTask(
        task = JudgeTask(
          submissionId = SubmissionId(claimedSubmission.id.value),
          problemSlug = ProblemSlug(claimedSubmission.problemSlug.value),
          startedAtEpochMilli = startedAtEpochMilli,
          programs = programs,
          problemDataVersion = manifest.version,
          roundingScale = roundingScale,
          aggregation = taskAggregation,
          subtasks = subtasks
        ),
        roleConfigs = roleConfigs
      )

  private def buildPrograms(
    claimedSubmission: ClaimedSubmission,
    sourceCodes: Map[String, domains.submission.objects.SubmissionSourceCode],
    roleConfigs: Map[String, RoleConfig],
    headers: List[JudgeTaskFileRef]
  ): Either[BuildError, Map[String, JudgeTaskProgram]] =
    sequenceBuild(claimedSubmission.programManifest.programs.toList.map { case (role, program) =>
      for
        sourceCode <- sourceCodes
          .get(role)
          .toRight(buildError(s"Source code for submission role $role was not found."))
        language = toProtocolLanguage(program.language)
        stub <- stubForSubmittedProgram(role, language, roleConfigs)
        programHeaders = if language == SubmissionLanguage.Cpp17 then headers else Nil
      yield role -> JudgeTaskProgram(language, SubmissionSourceCode(sourceCode.value), stub, programHeaders)
    }).map(_.toMap)

  private def validateSubmittedProgramLanguages(
    programs: Map[String, domains.submission.objects.SubmissionLanguage],
    roleConfigs: Map[String, RoleConfig]
  ): Either[BuildError, Unit] =
    sequenceBuild(programs.toList.map { case (role, language) =>
      stubForSubmittedProgram(role.trim, toProtocolLanguage(language), roleConfigs).void
    }).void

  private def stubForSubmittedProgram(
    role: String,
    language: SubmissionLanguage,
    roleConfigs: Map[String, RoleConfig]
  ): Either[BuildError, Option[JudgeTaskFileRef]] =
    roleConfigs.get(role) match
      case Some(roleConfig) if roleConfig.restrictsLanguages =>
        roleConfig.stubs
          .get(language)
          .toRight(
            buildError(
              s"Submission role $role declares stubs but does not support language ${SubmissionLanguage.render(language)}. " +
                s"Supported stub languages: ${roleConfig.stubs.keys.toList.map(SubmissionLanguage.render).sorted.mkString(", ")}."
            )
          )
          .map(Some(_))
      case _ =>
        Right(None)

  private def buildSubtask(
    manifest: ProblemDataManifest,
    raw: YamlObject,
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[CheckerConfig],
    parentValidator: Option[ToolConfig],
    parentStandard: StandardConfig,
    parentHack: Boolean,
    parentMode: JudgeTaskMode,
    parentStrategyProvider: Option[ToolConfig],
    parentAggregation: AggregationConfig
  ): Either[BuildError, JudgeTaskSubtask] =
    for
      label <- optionalStringAt(raw, "label").toBuildError
      subtaskLabel = judgeNodeLabel("subtask", index, label)
      _ <- rejectLegacyName(raw, subtaskLabel)
      limits <- limitsAt(raw).toBuildError.map(_.orElse(parentLimits))
      checker <- checkerAt(raw).toBuildError.map(_.orElse(parentChecker))
      validator <- toolAt(raw, "validator").toBuildError.map(_.orElse(parentValidator))
      resolvedValidator <- validator.traverse(resolveTool(manifest, _, "Validator source file")).toBuildError
      standard <- standardAt(raw).toBuildError.map(_.inherit(parentStandard))
      resolvedStandard <- standard.generator.traverse(resolveStandard(manifest, _)).toBuildError
      subtaskHack <- optionalBooleanAt(raw, "hack").toBuildError
      effectiveHack = subtaskHack.getOrElse(parentHack)
      _ <- validateHackConfig(subtaskLabel, effectiveHack, validator, standard)
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
          subtaskMode = mode,
          subtaskIndex = index,
          subtaskLabel = label
        )
      })
    yield
      JudgeTaskSubtask(
        index = index,
        label = label,
        scoreRatio = scoreRatio,
        mode = mode,
        validator = resolvedValidator,
        standard = resolvedStandard,
        hack = hackConfig(effectiveHack, standard),
        aggregation = aggregation.testcases.getOrElse(defaultAggregation),
        testcases = configuredTestcases
      )

  private def buildTestcase(
    manifest: ProblemDataManifest,
    raw: YamlObject,
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[CheckerConfig],
    parentStrategyProvider: Option[ToolConfig],
    subtaskMode: JudgeTaskMode,
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
      _ <- Either.cond(!raw.contains("hack"), (), buildError(s"hack cannot be declared on $label."))
      _ <- Either.cond(
        subtaskMode.`type` == "traditional" || !raw.contains("roles"),
        (),
        buildError(s"roles cannot be declared on $label when mode is interactive.")
      )
      roles <- optionalRoleListAt(raw, "roles", allowTextRoles = true).toBuildError
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
        testcaseType == JudgeTestcaseType.Hack || resolvedChecker.`type` != "builtin" || !resolvedChecker.name.contains("exact") || answerRef.nonEmpty,
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
        strategyProvider = resolvedStrategyProvider,
        roles = roles.getOrElse(Nil)
      )

  private def testcaseTypeAt(raw: YamlObject): Either[String, JudgeTestcaseType] =
    optionalStringAt(raw, "type").flatMap {
      case None => Right(JudgeTestcaseType.Main)
      case Some(value) => JudgeTestcaseType.parse(value)
    }

  private def rejectNonMainTestcaseScoreRatio(
    raw: YamlObject,
    testcaseType: JudgeTestcaseType,
    label: String
  ): Either[BuildError, Unit] =
    Either.cond(
      testcaseType == JudgeTestcaseType.Main || !raw.contains("scoreRatio"),
      (),
      buildError(s"scoreRatio cannot be declared on $label when type is ${JudgeTestcaseType.render(testcaseType)}.")
    )

  private def validateHackConfig(
    label: String,
    enabled: Boolean,
    validator: Option[ToolConfig],
    standard: StandardConfig
  ): Either[BuildError, Unit] =
    if !enabled then Right(())
    else
      for
        _ <- Either.cond(validator.nonEmpty, (), buildError(s"Validator is required for $label when hack is enabled."))
        _ <- standard match
          case StandardConfig.Unspecified => Left(buildError(s"standard must be declared as an answer generator object or false for $label when hack is enabled."))
          case StandardConfig.NoAnswer => Right(())
          case _: StandardConfig.Generator => Right(())
      yield ()

  private def hackConfig(enabled: Boolean, standard: StandardConfig): JudgeTaskHackConfig =
    if !enabled then JudgeTaskHackConfig.Disabled
    else
      standard match
        case _: StandardConfig.Generator => JudgeTaskHackConfig.WithAnswerGenerator
        case StandardConfig.NoAnswer => JudgeTaskHackConfig.WithoutAnswerGenerator
        case StandardConfig.Unspecified => JudgeTaskHackConfig.Disabled

  private def standardAt(raw: YamlObject): Either[String, StandardConfig] =
    raw.get("standard") match
      case None => Right(StandardConfig.Unspecified)
      case Some(YamlValue.Bool(false)) => Right(StandardConfig.NoAnswer)
      case Some(YamlValue.Obj(standardMap)) =>
        for
          language <- stringAt(standardMap, "language").flatMap(parseStandardLanguage)
          rawPath <- stringAt(standardMap, "path")
          path <- ProblemDataPath.parse(rawPath).left.map(message => s"Invalid standard path: $message")
        yield StandardConfig.Generator(language, path)
      case Some(_) => Left("standard must be an object or false.")

  private def parseStandardLanguage(raw: String): Either[String, SubmissionLanguage] =
    raw.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case "python3" => Right(SubmissionLanguage.Python3)
      case other => Left(s"Unsupported answer generator language: $other.")

  private def roleConfigsAt(raw: YamlObject, manifest: ProblemDataManifest): Either[String, Map[String, RoleConfig]] =
    optionalMapAt(raw, "roles").flatMap {
      case None => Right(Map.empty)
      case Some(roles) =>
        sequence(roles.toList.map { case (role, value) =>
          for
            validRole <- role.validateCodeRole.left.map(message => s"roles.$role $message")
            roleMap <- value match
              case YamlValue.Obj(map) => Right(map)
              case _ => Left(s"roles.$role must be an object.")
            roleConfig <- roleConfigAt(validRole, roleMap, manifest)
          yield validRole -> roleConfig
        }).map(_.toMap)
    }

  private def roleConfigAt(role: String, raw: YamlObject, manifest: ProblemDataManifest): Either[String, RoleConfig] =
    optionalMapAt(raw, "stubs").left.map(message => s"roles.$role.$message").flatMap {
      case None => Right(RoleConfig(Map.empty))
      case Some(stubs) =>
        sequence(stubs.toList.map { case (languageKey, value) =>
          for
            language <- parseStubLanguage(languageKey).left.map(message => s"roles.$role.stubs.$languageKey $message")
            path <- value match
              case YamlValue.Str(rawPath) if rawPath.trim.nonEmpty => Right(rawPath.trim)
              case YamlValue.Str(_) => Left(s"roles.$role.stubs.$languageKey must not be empty.")
              case _ => Left(s"roles.$role.stubs.$languageKey must be a string.")
            ref <- findFile(manifest, path, s"Stub source file for role $role language $languageKey")
          yield language -> ref
        }).map(items => RoleConfig(items.toMap))
    }

  private def headerRefsAt(raw: YamlObject, manifest: ProblemDataManifest): Either[String, List[JudgeTaskFileRef]] =
    optionalListOfStringsAt(raw, "headers").flatMap {
      case None => Right(Nil)
      case Some(paths) =>
        sequence(paths.zipWithIndex.map { case (path, index) =>
          findFile(manifest, path, s"Header file headers[$index]").flatMap { ref =>
            Either.cond(
              ref.path.value.toLowerCase(java.util.Locale.ROOT).endsWith(".h"),
              ref,
              s"Header file headers[$index] must end with .h: ${ref.path.value}."
            )
          }
        }).flatMap(validateUniqueHeaderBasenames)
    }

  private def validateUniqueHeaderBasenames(headers: List[JudgeTaskFileRef]): Either[String, List[JudgeTaskFileRef]] =
    val duplicateIncludeNames =
      headers
        .map(_.path.value.split('/').lastOption.getOrElse(""))
        .groupBy(identity)
        .collect { case (includeName, values) if values.size > 1 => includeName }
        .toList
        .sorted
    Either.cond(
      duplicateIncludeNames.isEmpty,
      headers,
      s"headers must not declare duplicate include names: ${duplicateIncludeNames.mkString(", ")}."
    )

  private def parseStubLanguage(raw: String): Either[String, SubmissionLanguage] =
    raw.trim match
      case "cpp17" => Right(SubmissionLanguage.Cpp17)
      case _ => Left("is not supported. Only cpp17 stubs are supported.")

  private def modeAt(raw: YamlObject, manifest: ProblemDataManifest): Either[String, Option[JudgeTaskMode]] =
    raw.get("mode") match
      case None => Right(None)
      case Some(YamlValue.Str(value)) if value.trim == "traditional" =>
        Right(Some(JudgeTaskMode.traditional(SubmissionProgramManifest.DefaultProgramKey)))
      case Some(YamlValue.Str(value)) if value.trim == "interactive" =>
        Left("mode must be an object when interactive mode is selected.")
      case Some(YamlValue.Obj(modeMap)) =>
        stringAt(modeMap, "type").flatMap {
          case "traditional" =>
            optionalStringAt(modeMap, "role").flatMap(_.getOrElse(SubmissionProgramManifest.DefaultProgramKey).validateTraditionalRole).map(role =>
              Some(JudgeTaskMode.traditional(role))
            )
          case "interactive" =>
            for
              rawRoles <- listOfStringsAt(modeMap, "roles")
              roles <- rawRoles.traverse(_.validateCodeRole)
              _ <- Either.cond(roles.nonEmpty, (), "interactive mode must declare at least one role.")
              interactor <- requiredLimitedToolFrom(modeMap.get("interactor"), "mode.interactor")
              resolvedInteractor <- resolveTool(manifest, interactor, "Interactor source file")
            yield Some(JudgeTaskMode.interactive(roles, resolvedInteractor))
          case other => Left(s"Unsupported judge mode: $other.")
        }
      case Some(_) => Left("mode must be a string or an object.")

  private def checkerAt(raw: YamlObject): Either[String, Option[CheckerConfig]] =
    optionalMapAt(raw, "checker").flatMap {
      case None => Right(None)
      case Some(checker) =>
        stringAt(checker, "type").flatMap {
          case "builtin" =>
            stringAt(checker, "name").flatMap {
              case "exact" => Right(Some(CheckerConfig.Builtin("exact")))
              case "echo" => Right(Some(CheckerConfig.Builtin("echo")))
              case other => Left(s"Unsupported builtin checker: $other.")
            }
          case "cpp17" | "cpp" =>
            stringAt(checker, "path").flatMap(rawPath =>
              ProblemDataPath.parse(rawPath)
                .left.map(message => s"Invalid checker path: $message")
                .map(path => Some(CheckerConfig.Cpp17(path)))
            )
          case other => Left(s"Unsupported checker type: $other.")
        }
    }

  private def toolAt(raw: YamlObject, key: String): Either[String, Option[ToolConfig]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value) => toolFrom(value, key).map(Some(_))

  private def limitedToolAt(raw: YamlObject, key: String): Either[String, Option[ToolConfig]] =
    raw.get(key) match
      case None => Right(None)
      case Some(value) => limitedToolFrom(value, key).map(Some(_))

  private def requiredLimitedToolFrom(value: Option[YamlValue], label: String): Either[String, ToolConfig] =
    value match
      case Some(currentValue) => limitedToolFrom(currentValue, label)
      case None => Left(s"$label is required.")

  private def toolFrom(value: YamlValue, label: String): Either[String, ToolConfig] =
    value match
      case YamlValue.Str(path) =>
        toolFromPath(path, label)
      case YamlValue.Obj(map) =>
        toolFromObject(map, label, requireLimits = false)
      case _ =>
        Left(s"$label must be a path string or an object with a path.")

  private def limitedToolFrom(value: YamlValue, label: String): Either[String, ToolConfig] =
    value match
      case YamlValue.Obj(map) =>
        toolFromObject(map, label, requireLimits = true)
      case _ =>
        Left(s"$label must be an object with path and limits.")

  private def toolFromObject(raw: YamlObject, label: String, requireLimits: Boolean): Either[String, ToolConfig] =
    for
      path <- stringAt(raw, "path")
      tool <- toolFromPath(path, label)
      limits <- toolLimitsAt(raw, label)
      _ <- Either.cond(!requireLimits || limits.nonEmpty, (), s"$label.limits is required.")
    yield tool.copy(limits = limits)

  private def toolFromPath(path: String, label: String): Either[String, ToolConfig] =
    ProblemDataPath.parse(path)
      .left.map(message => s"Invalid $label path: $message")
      .map(parsedPath => ToolConfig(parsedPath, None))

  private def limitsAt(raw: YamlObject): Either[String, Option[JudgeTaskLimits]] =
    optionalMapAt(raw, "limits").flatMap {
      case None => Right(None)
      case Some(limits) =>
        for
          timeMs <- intAt(limits, "timeMs").flatMap(TestcaseTimeLimitMs.parse)
          memoryMb <- intAt(limits, "memoryMb").flatMap(TestcaseMemoryLimitMb.parse)
        yield Some(JudgeTaskLimits(timeMs, memoryMb))
    }

  private def toolLimitsAt(raw: YamlObject, label: String): Either[String, Option[JudgeTaskToolLimits]] =
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

  private def aggregationAt(raw: YamlObject): Either[String, AggregationConfig] =
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
    ProblemDataPath.parse(rawPath)
      .left
      .map(message => s"$label has invalid path: $message")
      .flatMap(findFile(manifest, _, label))

  private def findFile(manifest: ProblemDataManifest, path: ProblemDataPath, label: String): Either[String, JudgeTaskFileRef] =
    for
      entry <- manifest.entries.find(_.path == path).toRight(s"$label does not exist: ${path.value}.")
      ref <- fileRef(entry).left.map(message => s"$label has invalid file reference: $message")
    yield ref

  private def resolveChecker(manifest: ProblemDataManifest, checker: CheckerConfig): Either[String, JudgeTaskChecker] =
    checker match
      case CheckerConfig.Builtin(name) => Right(JudgeTaskChecker("builtin", Some(name), None))
      case CheckerConfig.Cpp17(path) => findFile(manifest, path, "Checker source file").map(ref => JudgeTaskChecker("cpp17", None, Some(ref)))

  private def resolveTool(manifest: ProblemDataManifest, tool: ToolConfig, label: String): Either[String, JudgeTaskTool] =
    findFile(manifest, tool.path, label).map(ref => JudgeTaskTool(ref, tool.limits))

  private def resolveStandard(manifest: ProblemDataManifest, standard: StandardConfig.Generator): Either[String, JudgeTaskStandard] =
    findFile(manifest, standard.path, "Answer generator source file").map(ref => JudgeTaskStandard(standard.language, ref))

  private def fileRef(entry: ProblemDataManifestEntry): Either[String, JudgeTaskFileRef] =
    JudgeTaskFileRef.from(entry.path.value, entry.sizeBytes, entry.sha256)

  private def ratiosFor(items: List[YamlObject]): Either[String, List[BigDecimal]] =
    val explicit = items.map(optionalDecimalAt(_, "scoreRatio"))
    sequence(explicit).flatMap { ratios =>
      val missingCount = ratios.count(_.isEmpty)
      val explicitSum = ratios.flatten.sum
      if explicitSum > BigDecimal(1) then Left("scoreRatio values among siblings must not sum above 1.")
      else
        val fallback = if missingCount == 0 then BigDecimal(0) else (BigDecimal(1) - explicitSum) / BigDecimal(missingCount)
        Right(ratios.map(_.getOrElse(fallback)))
    }

  private def rejectLegacyName(raw: YamlObject, label: String): Either[BuildError, Unit] =
    Either.cond(!raw.contains("name"), (), buildError(s"$label must use label instead of name."))

  private def judgeNodeLabel(kind: String, index: Int, label: Option[String]): String =
    label match
      case Some(value) => s"$kind $index ($value)"
      case None => s"$kind $index"

  private def toYamlObject(value: Any, label: String): Either[String, YamlObject] =
    value match
      case map: java.util.Map[?, ?] =>
        map.asScala.toList.traverse {
          case (key: String, value) => toYamlValue(value, s"$label.$key").map(key -> _)
          case (key, _) => Left(s"$label contains non-string key: $key.")
        }.map(_.toMap)
      case _ => Left(s"$label must be an object.")

  private def toYamlValue(value: Any, label: String): Either[String, YamlValue] =
    value match
      case map: java.util.Map[?, ?] => toYamlObject(map, label).map(YamlValue.Obj.apply)
      case list: java.util.List[?] =>
        list.asScala.toList.zipWithIndex.traverse { case (item, index) => toYamlValue(item, s"$label[$index]") }.map(YamlValue.Arr.apply)
      case value: String => Right(YamlValue.Str(value))
      case value: java.lang.Boolean => Right(YamlValue.Bool(value.booleanValue))
      case value: java.lang.Integer => Right(YamlValue.Integral(BigInt(value.intValue)))
      case value: java.lang.Long => Right(YamlValue.Integral(BigInt(value.longValue)))
      case value: java.math.BigInteger => Right(YamlValue.Integral(BigInt(value)))
      case value: java.math.BigDecimal => Right(YamlValue.Decimal(BigDecimal(value)))
      case value: java.lang.Double if !value.isNaN && !value.isInfinite => Right(YamlValue.Decimal(BigDecimal(value.doubleValue)))
      case null => Right(YamlValue.NullValue)
      case other => Left(s"$label has unsupported YAML value type: ${other.getClass.getSimpleName}.")

  private def optionalMapAt(raw: YamlObject, key: String): Either[String, Option[YamlObject]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Obj(map)) => Right(Some(map))
      case Some(_) => Left(s"$key must be an object.")

  private def listOfMapsAt(raw: YamlObject, key: String): Either[String, List[YamlObject]] =
    raw.get(key) match
      case Some(YamlValue.Arr(items)) =>
        sequence(items.zipWithIndex.map {
          case (YamlValue.Obj(map), _) => Right(map)
          case (_, index) => Left(s"$key[$index] must be an object.")
        })
      case Some(_) => Left(s"$key must be a list.")
      case None => Left(s"$key is required.")

  private def listOfStringsAt(raw: YamlObject, key: String): Either[String, List[String]] =
    raw.get(key) match
      case Some(YamlValue.Arr(items)) =>
        sequence(items.zipWithIndex.map {
          case (YamlValue.Str(value), _) if value.trim.nonEmpty => Right(value.trim)
          case (YamlValue.Str(_), index) => Left(s"$key[$index] must not be empty.")
          case (_, index) => Left(s"$key[$index] must be a string.")
        })
      case Some(_) => Left(s"$key must be a list.")
      case None => Left(s"$key is required.")

  private def optionalListOfStringsAt(raw: YamlObject, key: String): Either[String, Option[List[String]]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Arr(items)) =>
        sequence(items.zipWithIndex.map {
          case (YamlValue.Str(value), _) if value.trim.nonEmpty => Right(value.trim)
          case (YamlValue.Str(_), index) => Left(s"$key[$index] must not be empty.")
          case (_, index) => Left(s"$key[$index] must be a string.")
        }).map(Some(_))
      case Some(_) => Left(s"$key must be a list.")

  private def optionalRoleListAt(raw: YamlObject, key: String, allowTextRoles: Boolean): Either[String, Option[List[String]]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Arr(items)) =>
        sequence(items.zipWithIndex.map {
          case (YamlValue.Str(value), index) if value.trim.nonEmpty =>
            val label = s"$key[$index]"
            val role = value.trim
            if allowTextRoles then role.validateTraditionalRole.left.map(message => s"$label $message")
            else role.validateCodeRole.left.map(message => s"$label $message")
          case (YamlValue.Str(_), index) => Left(s"$key[$index] must not be empty.")
          case (_, index) => Left(s"$key[$index] must be a string.")
        }).flatMap { roles =>
          Either.cond(roles.nonEmpty, Some(roles), s"$key must contain at least one role.")
        }
      case Some(_) => Left(s"$key must be a list.")

  private def stringAt(raw: YamlObject, key: String): Either[String, String] =
    optionalStringAt(raw, key).flatMap(_.toRight(s"$key is required."))

  private def optionalStringAt(raw: YamlObject, key: String): Either[String, Option[String]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Str(value)) if value.trim.nonEmpty => Right(Some(value.trim))
      case Some(YamlValue.Str(_)) => Left(s"$key must not be empty.")
      case Some(_) => Left(s"$key must be a string.")

  private def intAt(raw: YamlObject, key: String): Either[String, Int] =
    optionalIntAt(raw, key).flatMap(_.toRight(s"$key is required."))

  private def optionalIntAt(raw: YamlObject, key: String): Either[String, Option[Int]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Integral(value)) if value.isValidInt => Right(Some(value.toInt))
      case Some(value) => Left(s"$key must be an integer, found ${yamlTypeName(value)}.")

  private def optionalBooleanAt(raw: YamlObject, key: String): Either[String, Option[Boolean]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Bool(value)) => Right(Some(value))
      case Some(_) => Left(s"$key must be a boolean.")

  private def optionalDecimalAt(raw: YamlObject, key: String): Either[String, Option[BigDecimal]] =
    raw.get(key) match
      case None => Right(None)
      case Some(YamlValue.Integral(value)) => validateRatio(BigDecimal(value), key).map(Some(_))
      case Some(YamlValue.Decimal(value)) => validateRatio(value, key).map(Some(_))
      case Some(value) => Left(s"$key must be a number, found ${yamlTypeName(value)}.")

  private def yamlTypeName(value: YamlValue): String =
    value match
      case YamlValue.Obj(_) => "object"
      case YamlValue.Arr(_) => "list"
      case YamlValue.Str(_) => "string"
      case YamlValue.Bool(_) => "boolean"
      case YamlValue.Integral(_) | YamlValue.Decimal(_) => "number"
      case YamlValue.NullValue => "null"

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
    private def validateCodeRole: Either[String, String] =
      Either.cond(
        CodeRolePattern.findFirstIn(role).contains(role),
        role,
        s"Role must contain only ASCII letters, digits, '_' or '-': $role."
      )

    private def validateTraditionalRole: Either[String, String] =
      Either.cond(
        CodeRolePattern.findFirstIn(role).contains(role) || TextRolePattern.findFirstIn(role).contains(role),
        role,
        s"Role must contain only ASCII letters, digits, '_' or '-', with an optional single '.txt' suffix: $role."
      )

  extension [A](option: Option[Either[String, A]])
    private def sequence: Either[String, Option[A]] =
      option match
        case Some(value) => value.map(Some(_))
        case None => Right(None)
