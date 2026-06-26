package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.request.MaterializeHackProblemDataInput
import domains.problem.table.problem.ProblemMutationTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import org.snakeyaml.engine.v2.api.{Dump, DumpSettings, Load, LoadSettings}
import org.snakeyaml.engine.v2.common.FlowStyle
import org.http4s.Method
import shared.api.ApiPath

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.time.Instant
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** 内部 hack 数据物化 API；把成功 hack 写入题目数据文件、更新 judge.yaml，并提升题目重判 revision。 */
final case class MaterializeHackProblemData(problemDataStorage: ProblemDataStorageContext) extends InternalOnlyApi[MaterializeHackProblemDataInput, Unit]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/hack-data")

  /** 执行物化副作用；失败时恢复对象存储快照，数据库改动依赖调用方事务回滚。 */
  override def plan(connection: Connection, input: MaterializeHackProblemDataInput): IO[Unit] =
    MaterializeHackProblemData.materialize(connection, problemDataStorage, input)

/** hack 数据物化的纯辅助逻辑与内部构造器；对外只暴露输入构造和可测的 judge.yaml 追加函数。 */
object MaterializeHackProblemData:

  private val JudgeYamlPath = ProblemDataPath("judge.yaml")

  private enum YamlValue:
    case Obj(value: YamlObject)
    case Arr(values: Vector[YamlValue])
    case Str(value: String)
    case Bool(value: Boolean)
    case Integral(value: BigInt)
    case Decimal(value: BigDecimal)
    case NullValue

  private final case class YamlObject(fields: Vector[(String, YamlValue)]):
    def valueAt(key: String): Option[YamlValue] =
      fields.find(_._1 == key).map(_._2)

    def updated(key: String, value: YamlValue): YamlObject =
      val (before, after) = fields.span(_._1 != key)
      if after.isEmpty then YamlObject(fields :+ (key -> value))
      else YamlObject(before ++ Vector(key -> value) ++ after.drop(1))

  /** 构造物化请求输入；调用方负责保证路径来自受控命名空间且文本已通过 hack 校验。 */
  def input(
    problemId: ProblemId,
    problemSlug: ProblemSlug,
    subtaskIndex: Int,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath],
    testcaseLabel: String,
    inputText: String,
    answerText: Option[String],
    createdAt: Instant
  ): MaterializeHackProblemDataInput =
    MaterializeHackProblemDataInput(
      problemId = problemId,
      problemSlug = problemSlug,
      subtaskIndex = subtaskIndex,
      inputPath = inputPath,
      answerPath = answerPath,
      testcaseLabel = testcaseLabel,
      inputText = inputText,
      answerText = answerText,
      createdAt = createdAt
    )

  private def materialize(
    connection: Connection,
    problemDataStorage: ProblemDataStorageContext,
    input: MaterializeHackProblemDataInput
  ): IO[Unit] =
    for
      _ <- lockProblem(connection, input.problemId)
      snapshot <- ProblemDataStorage.snapshotDirectory(problemDataStorage, input.problemSlug)
      inputBytes = input.inputText.getBytes(StandardCharsets.UTF_8)
      answerBytes = input.answerText.map(_.getBytes(StandardCharsets.UTF_8))
      action =
        for
          judgeYamlBytes <- readJudgeYaml(problemDataStorage, input.problemSlug)
          updatedJudgeYaml = appendHackTestcaseToJudgeYaml(
            judgeYamlBytes = judgeYamlBytes,
            subtaskIndex = input.subtaskIndex,
            testcaseLabel = input.testcaseLabel,
            inputPath = input.inputPath,
            answerPath = input.answerPath
          )
          entries = List(
            Some(manifestEntry(input.inputPath, inputBytes)),
            input.answerPath.zip(answerBytes).map { case (path, bytes) => manifestEntry(path, bytes) },
            Some(manifestEntry(JudgeYamlPath, updatedJudgeYaml))
          ).flatten
          _ <- ProblemDataStorage.writePath(problemDataStorage, input.problemSlug, input.inputPath, inputBytes)
          _ <- input.answerPath.zip(answerBytes).traverse_ { case (path, bytes) =>
            ProblemDataStorage.writePath(problemDataStorage, input.problemSlug, path, bytes)
          }
          _ <- ProblemDataStorage.writePath(problemDataStorage, input.problemSlug, JudgeYamlPath, updatedJudgeYaml)
          _ <- ProblemDataFileTable.upsertForProblem(connection, input.problemId, entries, input.createdAt)
          _ <- ProblemMutationTable.incrementRejudgeRevision(connection, input.problemId)
        yield ()
      _ <- action.handleErrorWith(error => ProblemDataStorage.restoreDirectory(problemDataStorage, input.problemSlug, snapshot) *> IO.raiseError(error))
    yield ()

  private[api] def appendHackTestcaseToJudgeYaml(
    judgeYamlBytes: Array[Byte],
    subtaskIndex: Int,
    testcaseLabel: String,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath]
  ): Array[Byte] =
    val root = parseJudgeYaml(judgeYamlBytes).flatMap(appendHackTestcase(_, subtaskIndex, testcaseLabel, inputPath, answerPath)) match
      case Right(updatedRoot) => updatedRoot
      case Left(message) => throw IllegalArgumentException(message)

    val settings =
      DumpSettings
        .builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setSplitLines(false)
        .build()
    val rendered = Dump(settings).dumpToString(toJavaYamlObject(root))
    val normalized = if rendered.endsWith("\n") then rendered else s"$rendered\n"
    normalized.getBytes(StandardCharsets.UTF_8)

  private def readJudgeYaml(
    problemDataStorage: ProblemDataStorageContext,
    problemSlug: ProblemSlug
  ): IO[Array[Byte]] =
    ProblemDataStorage.readPath(problemDataStorage, problemSlug, JudgeYamlPath).flatMap {
      case Some((_, bytes)) => IO.pure(bytes)
      case None => IO.raiseError(IllegalStateException("judge.yaml is required before hack materialization."))
    }

  private def lockProblem(
    connection: Connection,
    problemId: ProblemId
  ): IO[Unit] =
    ProblemMutationTable.lockExisting(connection, problemId).flatMap { exists =>
      if exists then IO.unit
      else IO.raiseError(IllegalStateException("Problem disappeared before hack materialization."))
    }

  private def parseJudgeYaml(bytes: Array[Byte]): Either[String, YamlObject] =
    Try {
      val settings = LoadSettings.builder().setLabel("judge.yaml").build()
      Load(settings).loadFromString(new String(bytes, StandardCharsets.UTF_8))
    }.toEither match
      case Right(map: java.util.Map[?, ?]) =>
        yamlObjectFrom(map, "judge.yaml")
      case Right(_) => Left("judge.yaml must be an object before hack materialization.")
      case Left(error) => Left(s"Invalid judge.yaml: ${error.getMessage}")

  private def yamlObjectFrom(map: java.util.Map[?, ?], label: String): Either[String, YamlObject] =
    map.asScala.toVector.traverse {
      case (key: String, value) => yamlValueFrom(value, s"$label.$key").map(key -> _)
      case (key, _) => Left(s"$label contains non-string key: $key.")
    }.map(YamlObject(_))

  private def yamlValueFrom(value: Any, label: String): Either[String, YamlValue] =
    value match
      case map: java.util.Map[?, ?] => yamlObjectFrom(map, label).map(YamlValue.Obj.apply)
      case list: java.util.List[?] => list.asScala.toVector.zipWithIndex.traverse { case (item, index) => yamlValueFrom(item, s"$label[$index]") }.map(YamlValue.Arr.apply)
      case scalar: String => Right(YamlValue.Str(scalar))
      case scalar: java.lang.Boolean => Right(YamlValue.Bool(scalar.booleanValue))
      case scalar: java.lang.Integer => Right(YamlValue.Integral(BigInt(scalar.intValue)))
      case scalar: java.lang.Long => Right(YamlValue.Integral(BigInt(scalar.longValue)))
      case scalar: java.math.BigInteger => Right(YamlValue.Integral(BigInt(scalar)))
      case scalar: java.math.BigDecimal => Right(YamlValue.Decimal(BigDecimal(scalar)))
      case scalar: java.lang.Double if !scalar.isNaN && !scalar.isInfinite => Right(YamlValue.Decimal(BigDecimal(scalar.doubleValue)))
      case null => Right(YamlValue.NullValue)
      case scalar => Left(s"$label has unsupported YAML value type: ${scalar.getClass.getSimpleName}.")

  private def appendHackTestcase(
    root: YamlObject,
    subtaskIndex: Int,
    testcaseLabel: String,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath]
  ): Either[String, YamlObject] =
    if subtaskIndex <= 0 then Left(s"Invalid hack subtask index: $subtaskIndex.")
    else
      root.valueAt("subtasks") match
        case Some(YamlValue.Arr(subtasks)) =>
          subtasks.lift(subtaskIndex - 1) match
            case Some(YamlValue.Obj(subtask)) =>
              subtask.valueAt("testcases") match
                case Some(YamlValue.Arr(testcases)) =>
                  val updatedSubtask = subtask.updated("testcases", YamlValue.Arr(testcases :+ hackTestcaseValue(testcaseLabel, inputPath, answerPath)))
                  val updatedSubtasks = subtasks.updated(subtaskIndex - 1, YamlValue.Obj(updatedSubtask))
                  Right(root.updated("subtasks", YamlValue.Arr(updatedSubtasks)))
                case None => Left(s"subtask $subtaskIndex must define testcases before hack materialization.")
                case Some(_) => Left(s"subtask $subtaskIndex testcases must be a list before hack materialization.")
            case Some(_) => Left(s"subtask $subtaskIndex must be an object before hack materialization.")
            case None => Left(s"subtask $subtaskIndex does not exist before hack materialization.")
        case None => Left("judge.yaml must define subtasks before hack materialization.")
        case Some(_) => Left("judge.yaml subtasks must be a list before hack materialization.")

  private def hackTestcaseValue(
    testcaseLabel: String,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath]
  ): YamlValue =
    YamlValue.Obj(
      YamlObject(
        Vector(
          "label" -> YamlValue.Str(testcaseLabel),
          "type" -> YamlValue.Str("hack"),
          "input" -> YamlValue.Str(inputPath.value)
        ) ++ answerPath.map(path => "answer" -> YamlValue.Str(path.value)).toVector
      )
    )

  private def toJavaYamlObject(value: YamlObject): java.util.Map[String, Object] =
    ListMap.from(value.fields.map { case (key, fieldValue) => key -> toJavaYamlValue(fieldValue) }).asJava

  private def toJavaYamlValue(value: YamlValue): Object =
    value match
      case YamlValue.Obj(obj) => toJavaYamlObject(obj)
      case YamlValue.Arr(values) => values.map(toJavaYamlValue).asJava
      case YamlValue.Str(scalar) => scalar
      case YamlValue.Bool(scalar) => java.lang.Boolean.valueOf(scalar)
      case YamlValue.Integral(scalar) => scalar.bigInteger
      case YamlValue.Decimal(scalar) => scalar.bigDecimal
      case YamlValue.NullValue => null

  private def manifestEntry(path: ProblemDataPath, bytes: Array[Byte]): ProblemDataManifestEntry =
    ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
