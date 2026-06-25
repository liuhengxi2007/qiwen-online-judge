package domains.problem.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemDataPath, ProblemId, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.objects.request.MaterializeHackProblemDataInput
import domains.problem.table.problem.ProblemMutationTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.utils.{ProblemDataStorage, ProblemDataStorageContext}
import org.snakeyaml.engine.v2.api.{Dump, DumpSettings, Load, LoadSettings}
import org.snakeyaml.engine.v2.common.FlowStyle
import org.http4s.Method
import shared.api.ApiPath

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.time.Instant
import java.util.{ArrayList, LinkedHashMap}
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
    val root = parseJudgeYaml(judgeYamlBytes)
    val subtask = subtaskAt(root, subtaskIndex)
    val existingTestcases = subtask.get("testcases") match
      case testcases: java.util.List[?] =>
        val copy = new ArrayList[Any]()
        copy.addAll(testcases.asScala.toList.asJava)
        copy
      case null => throw IllegalArgumentException(s"subtask $subtaskIndex must define testcases before hack materialization.")
      case _ => throw IllegalArgumentException(s"subtask $subtaskIndex testcases must be a list before hack materialization.")
    existingTestcases.add(hackTestcaseMap(testcaseLabel, inputPath, answerPath))
    subtask.put("testcases", existingTestcases)

    val settings =
      DumpSettings
        .builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setSplitLines(false)
        .build()
    val rendered = Dump(settings).dumpToString(root)
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

  private def parseJudgeYaml(bytes: Array[Byte]): LinkedHashMap[String, Any] =
    Try {
      val settings = LoadSettings.builder().setLabel("judge.yaml").build()
      Load(settings).loadFromString(new String(bytes, StandardCharsets.UTF_8))
    }.toEither match
      case Right(map: java.util.Map[?, ?]) =>
        val root = new LinkedHashMap[String, Any]()
        map.asScala.foreach {
          case (key: String, value) => root.put(key, value)
          case (key, _) => throw IllegalArgumentException(s"judge.yaml root contains non-string key: $key.")
        }
        root
      case Right(_) => throw IllegalArgumentException("judge.yaml must be an object before hack materialization.")
      case Left(error) => throw IllegalArgumentException(s"Invalid judge.yaml: ${error.getMessage}", error)

  private def subtaskAt(root: LinkedHashMap[String, Any], subtaskIndex: Int): java.util.Map[String, Any] =
    if subtaskIndex <= 0 then throw IllegalArgumentException(s"Invalid hack subtask index: $subtaskIndex.")
    val subtasks = root.get("subtasks") match
      case list: java.util.List[?] => list.asScala.toList
      case null => throw IllegalArgumentException("judge.yaml must define subtasks before hack materialization.")
      case _ => throw IllegalArgumentException("judge.yaml subtasks must be a list before hack materialization.")
    subtasks.lift(subtaskIndex - 1) match
      case Some(map: java.util.Map[?, ?]) =>
        map.asInstanceOf[java.util.Map[String, Any]]
      case Some(_) => throw IllegalArgumentException(s"subtask $subtaskIndex must be an object before hack materialization.")
      case None => throw IllegalArgumentException(s"subtask $subtaskIndex does not exist before hack materialization.")

  private def hackTestcaseMap(
    testcaseLabel: String,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath]
  ): LinkedHashMap[String, Any] =
    val testcase = new LinkedHashMap[String, Any]()
    testcase.put("label", testcaseLabel)
    testcase.put("type", "hack")
    testcase.put("input", inputPath.value)
    answerPath.foreach(path => testcase.put("answer", path.value))
    testcase

  private def manifestEntry(path: ProblemDataPath, bytes: Array[Byte]): ProblemDataManifestEntry =
    ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
