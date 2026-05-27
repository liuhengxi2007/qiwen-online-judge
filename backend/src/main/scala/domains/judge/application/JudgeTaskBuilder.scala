package domains.judge.application

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.application.{ProblemCommands, ProblemDataStorage}
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.submission.objects.internal.ClaimedSubmission
import judgeprotocol.objects.{JudgeTask, JudgeTaskAggregation, JudgeTaskChecker, JudgeTaskFileRef, JudgeTaskLimits, JudgeTaskSubtask, JudgeTaskTestcase, ProblemSlug, ProblemSpaceLimitMb, ProblemTimeLimitMs, SubmissionId, SubmissionLanguage, SubmissionSourceCode, TestcaseName}
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import scala.jdk.CollectionConverters.*
import scala.util.Try

object JudgeTaskBuilder:

  final case class ReadyValidation(
    retainedPaths: Set[ProblemDataPath]
  )

  def buildJudgeTask(
    connection: java.sql.Connection,
    problemDataStorage: ProblemDataStorage,
    claimedSubmission: ClaimedSubmission
  ): IO[Either[String, JudgeTask]] =
    for
      maybeManifest <- ProblemCommands.judgeTaskManifest(connection, claimedSubmission.problemId, claimedSubmission.problemSlug)
      manifest = maybeManifest.getOrElse(ProblemDataManifest.fromEntries(claimedSubmission.problemSlug, Nil))
      config <- loadConfig(problemDataStorage, claimedSubmission, manifest)
    yield
      maybeManifest match
        case None =>
          Left("Problem not found for claimed submission.")
        case Some(_) =>
          config

  private def toProtocolLanguage(language: domains.submission.objects.SubmissionLanguage): SubmissionLanguage =
    language match
      case domains.submission.objects.SubmissionLanguage.Cpp17 => SubmissionLanguage.Cpp17
      case domains.submission.objects.SubmissionLanguage.Python3 => SubmissionLanguage.Python3

  private def loadConfig(
    problemDataStorage: ProblemDataStorage,
    claimedSubmission: ClaimedSubmission,
    manifest: ProblemDataManifest
  ): IO[Either[String, JudgeTask]] =
    ProblemDataPath.parse("judge.yaml") match
      case Left(message) => IO.pure(Left(message))
      case Right(configPath) =>
        problemDataStorage.readPath(claimedSubmission.problemSlug, configPath).map {
          case None => Left("judge.yaml is required at the problem data root.")
          case Some((_, bytes)) =>
            parseConfigBytes(bytes, claimedSubmission, manifest)
        }

  private[application] def parseConfigBytes(
    bytes: Array[Byte],
    claimedSubmission: ClaimedSubmission,
    manifest: ProblemDataManifest
  ): Either[String, JudgeTask] =
    parseYaml(bytes).flatMap { root =>
      buildFromYaml(claimedSubmission, manifest, root)
    }

  def validateReadyConfigBytes(
    bytes: Array[Byte],
    problem: domains.problem.objects.response.ProblemDetail,
    manifest: ProblemDataManifest
  ): Either[String, ReadyValidation] =
    val claimedSubmission = ClaimedSubmission(
      id = domains.submission.objects.SubmissionId(0L),
      problemId = problem.id,
      problemSlug = problem.slug,
      language = domains.submission.objects.SubmissionLanguage.Cpp17,
      sourceCode = domains.submission.objects.SubmissionSourceCode("int main() { return 0; }"),
      timeLimitMs = problem.timeLimitMs,
      spaceLimitMb = problem.spaceLimitMb
    )
    parseConfigBytes(bytes, claimedSubmission, manifest).flatMap { task =>
      val rawPaths =
        task.subtasks.flatMap(_.testcases).flatMap { testcase =>
          List(testcase.input.map(_.path), Some(testcase.answer.path), testcase.checker.source.map(_.path)).flatten
        }
      rawPaths
        .traverse(ProblemDataPath.parse)
        .map(paths => ReadyValidation(paths.toSet + ProblemDataPath("judge.yaml")))
    }

  private def parseYaml(bytes: Array[Byte]): Either[String, Map[String, Any]] =
    Try {
        val settings = LoadSettings.builder().setLabel("judge.yaml").build()
        val loaded = Load(settings).loadFromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
        toScalaMap(loaded)
      }
      .toEither
      .left
      .map(error => s"Invalid judge.yaml: ${error.getMessage}")

  private def buildFromYaml(
    claimedSubmission: ClaimedSubmission,
    manifest: ProblemDataManifest,
    root: Map[String, Any]
  ): Either[String, JudgeTask] =
    for
      version <- intAt(root, "version").flatMap(value => Either.cond(value == 1, value, "judge.yaml version must be 1."))
      roundingScale <- optionalIntAt(root, "roundingScale").map(_.getOrElse(6))
      _ <- Either.cond(roundingScale >= 0 && roundingScale <= 18, (), "roundingScale must be between 0 and 18.")
      rootLimits <- limitsAt(root).map(_.orElse(Some(JudgeTaskLimits(ProblemTimeLimitMs(claimedSubmission.timeLimitMs.value), ProblemSpaceLimitMb(claimedSubmission.spaceLimitMb.value)))))
      rootChecker <- checkerAt(root)
      rootAggregation <- aggregationAt(root)
      subtaskMaps <- listOfMapsAt(root, "subtasks")
      _ <- Either.cond(subtaskMaps.nonEmpty, (), "judge.yaml must define at least one subtask.")
      subtaskRatios <- ratiosFor(subtaskMaps)
      subtasks <- sequence(subtaskMaps.zip(subtaskRatios).zipWithIndex.map { case ((subtaskMap, subtaskRatio), subtaskIndex) =>
        buildSubtask(manifest, subtaskMap, subtaskIndex, subtaskRatio, rootLimits, rootChecker, rootAggregation)
      })
      taskAggregation = rootAggregation.subtasks.getOrElse(defaultAggregation)
    yield
      JudgeTask(
        submissionId = SubmissionId(claimedSubmission.id.value),
        problemSlug = ProblemSlug(claimedSubmission.problemSlug.value),
        language = toProtocolLanguage(claimedSubmission.language),
        sourceCode = SubmissionSourceCode(claimedSubmission.sourceCode.value),
        problemDataVersion = manifest.version,
        roundingScale = roundingScale,
        aggregation = taskAggregation,
        subtasks = subtasks
      )

  private def buildSubtask(
    manifest: ProblemDataManifest,
    raw: Map[String, Any],
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[JudgeTaskChecker],
    parentAggregation: AggregationConfig
  ): Either[String, JudgeTaskSubtask] =
    for
      name <- optionalStringAt(raw, "name").map(_.getOrElse(s"subtask-${index + 1}"))
      limits <- limitsAt(raw).map(_.orElse(parentLimits))
      checker <- checkerAt(raw).map(_.orElse(parentChecker))
      aggregation <- aggregationAt(raw).map(parentAggregation.merge)
      testcaseMaps <- listOfMapsAt(raw, "testcases")
      _ <- Either.cond(testcaseMaps.nonEmpty, (), s"Subtask $name must define at least one testcase.")
      testcaseRatios <- ratiosFor(testcaseMaps)
      testcases <- sequence(testcaseMaps.zip(testcaseRatios).zipWithIndex.map { case ((testcaseMap, testcaseRatio), testcaseIndex) =>
        buildTestcase(manifest, testcaseMap, testcaseIndex, testcaseRatio, limits, checker, name)
      })
    yield JudgeTaskSubtask(name = name, scoreRatio = scoreRatio, aggregation = aggregation.testcases.getOrElse(defaultAggregation), testcases = testcases)

  private def buildTestcase(
    manifest: ProblemDataManifest,
    raw: Map[String, Any],
    index: Int,
    scoreRatio: BigDecimal,
    parentLimits: Option[JudgeTaskLimits],
    parentChecker: Option[JudgeTaskChecker],
    subtaskName: String
  ): Either[String, JudgeTaskTestcase] =
    for
      name <- optionalStringAt(raw, "name").map(_.getOrElse(s"${index + 1}"))
      limits <- limitsAt(raw).map(_.orElse(parentLimits)).flatMap(_.toRight(s"Limits are required for testcase $subtaskName/$name."))
      checker <- checkerAt(raw).map(_.orElse(parentChecker)).flatMap(_.toRight(s"Checker is required for testcase $subtaskName/$name."))
      resolvedChecker <- resolveChecker(manifest, checker)
      inputPath <- optionalStringAt(raw, "input")
      answerPath <- optionalStringAt(raw, "answer").flatMap(_.toRight(s"Answer file is required for testcase $subtaskName/$name."))
      inputRef <- inputPath match
        case None => Right(None)
        case Some(path) => findFile(manifest, path, s"Input file for testcase $subtaskName/$name").map(Some(_))
      answerRef <- findFile(manifest, answerPath, s"Answer file for testcase $subtaskName/$name")
    yield JudgeTaskTestcase(TestcaseName(name), scoreRatio, limits, resolvedChecker, inputRef, answerRef)

  private def checkerAt(raw: Map[String, Any]): Either[String, Option[JudgeTaskChecker]] =
    optionalMapAt(raw, "checker").flatMap {
      case None => Right(None)
      case Some(checker) =>
        stringAt(checker, "type").flatMap {
          case "builtin" =>
            stringAt(checker, "name").flatMap {
              case "exact" => Right(Some(JudgeTaskChecker("builtin", Some("exact"), None)))
              case other => Left(s"Unsupported builtin checker: $other.")
            }
          case "cpp" =>
            stringAt(checker, "path").flatMap(path =>
              ProblemDataPath.parse(path)
                .left.map(message => s"Invalid checker path: $message")
                .map(_ => Some(JudgeTaskChecker("cpp", None, Some(JudgeTaskFileRef(path, 0L, "")))))
            )
          case other => Left(s"Unsupported checker type: $other.")
        }
    }

  private def limitsAt(raw: Map[String, Any]): Either[String, Option[JudgeTaskLimits]] =
    optionalMapAt(raw, "limits").flatMap {
      case None => Right(None)
      case Some(limits) =>
        for
          timeMs <- intAt(limits, "timeMs").flatMap(ProblemTimeLimitMs.parse)
          memoryMb <- intAt(limits, "memoryMb").flatMap(ProblemSpaceLimitMb.parse)
        yield Some(JudgeTaskLimits(timeMs, memoryMb))
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

  private val allowedAggregations = Vector(
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
    yield fileRef(entry)

  private def resolveChecker(manifest: ProblemDataManifest, checker: JudgeTaskChecker): Either[String, JudgeTaskChecker] =
    checker.`type` match
      case "cpp" =>
        checker.source match
          case Some(source) => findFile(manifest, source.path, "Checker source file").map(ref => checker.copy(source = Some(ref)))
          case None => Left("C++ checker source path is required.")
      case _ => Right(checker)

  private def fileRef(entry: ProblemDataManifestEntry): JudgeTaskFileRef =
    JudgeTaskFileRef(entry.path.value, entry.sizeBytes, entry.sha256)

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

  extension [A](option: Option[Either[String, A]])
    private def sequence: Either[String, Option[A]] =
      option match
        case Some(value) => value.map(Some(_))
        case None => Right(None)
