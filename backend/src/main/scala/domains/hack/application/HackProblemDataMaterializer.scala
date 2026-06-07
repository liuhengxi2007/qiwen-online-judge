package domains.hack.application

import cats.effect.IO
import cats.syntax.all.*
import domains.hack.objects.HackId
import domains.hack.table.hack.HackMutationTable
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.ProblemDataManifestEntry
import domains.problem.table.problem_data_file.ProblemDataFileTable
import domains.problem.utils.ProblemDataStorage
import org.snakeyaml.engine.v2.api.{Dump, DumpSettings, Load, LoadSettings}
import org.snakeyaml.engine.v2.common.FlowStyle

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.time.Instant
import java.util.{ArrayList, LinkedHashMap}
import scala.jdk.CollectionConverters.*
import scala.util.Try

object HackProblemDataMaterializer:

  private val JudgeYamlPath = ProblemDataPath("judge.yaml")

  def materializeSuccessfulHack(
    connection: Connection,
    problemDataStorage: ProblemDataStorage,
    hackId: HackId,
    source: HackMutationTable.CompletedAttemptSource,
    answer: Option[String],
    createdAt: Instant
  ): IO[Unit] =
    for
      inputPath <- problemDataPath(inputPathFor(hackId))
      answerPath <- answer.traverse(_ => problemDataPath(answerPathFor(hackId)))
      _ <- lockProblem(connection, source)
      snapshot <- problemDataStorage.snapshotDirectory(source.problemSlug)
      inputBytes = source.input.getBytes(StandardCharsets.UTF_8)
      answerBytes = answer.map(_.getBytes(StandardCharsets.UTF_8))
      action =
        for
          judgeYamlBytes <- readJudgeYaml(problemDataStorage, source)
          updatedJudgeYaml = appendHackTestcaseToJudgeYaml(
            judgeYamlBytes = judgeYamlBytes,
            subtaskIndex = source.subtaskIndex,
            hackId = hackId,
            inputPath = inputPath,
            answerPath = answerPath
          )
          entries = List(
            Some(manifestEntry(inputPath, inputBytes)),
            answerPath.zip(answerBytes).map { case (path, bytes) => manifestEntry(path, bytes) },
            Some(manifestEntry(JudgeYamlPath, updatedJudgeYaml))
          ).flatten
          _ <- problemDataStorage.writePath(source.problemSlug, inputPath, inputBytes)
          _ <- answerPath.zip(answerBytes).traverse_ { case (path, bytes) =>
            problemDataStorage.writePath(source.problemSlug, path, bytes)
          }
          _ <- problemDataStorage.writePath(source.problemSlug, JudgeYamlPath, updatedJudgeYaml)
          _ <- ProblemDataFileTable.upsertForProblem(connection, source.problemId, entries, createdAt)
          _ <- HackMutationTable.incrementProblemHackRevision(connection, source.problemId)
        yield ()
      _ <- action.handleErrorWith(error => problemDataStorage.restoreDirectory(source.problemSlug, snapshot) *> IO.raiseError(error))
    yield ()

  private[hack] def appendHackTestcaseToJudgeYaml(
    judgeYamlBytes: Array[Byte],
    subtaskIndex: Int,
    hackId: HackId,
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
    existingTestcases.add(hackTestcaseMap(hackId, inputPath, answerPath))
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
    problemDataStorage: ProblemDataStorage,
    source: HackMutationTable.CompletedAttemptSource
  ): IO[Array[Byte]] =
    problemDataStorage.readPath(source.problemSlug, JudgeYamlPath).flatMap {
      case Some((_, bytes)) => IO.pure(bytes)
      case None => IO.raiseError(IllegalStateException("judge.yaml is required before hack materialization."))
    }

  private val lockProblemSQL: String =
    """
      |select 1
      |from problems
      |where id = ?
      |for update
      |""".stripMargin

  private def lockProblem(
    connection: Connection,
    source: HackMutationTable.CompletedAttemptSource
  ): IO[Unit] =
    IO.blocking {
      val statement = connection.prepareStatement(lockProblemSQL)
      try
        statement.setObject(1, source.problemId.value)
        val resultSet = statement.executeQuery()
        try
          if !resultSet.next() then throw IllegalStateException("Problem disappeared before hack materialization.")
        finally resultSet.close()
      finally statement.close()
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
    hackId: HackId,
    inputPath: ProblemDataPath,
    answerPath: Option[ProblemDataPath]
  ): LinkedHashMap[String, Any] =
    val testcase = new LinkedHashMap[String, Any]()
    testcase.put("label", s"hack #${hackId.value}")
    testcase.put("type", "hack")
    testcase.put("input", inputPath.value)
    answerPath.foreach(path => testcase.put("answer", path.value))
    testcase

  private def problemDataPath(value: String): IO[ProblemDataPath] =
    IO.fromEither(ProblemDataPath.parse(value).left.map(IllegalArgumentException(_)))

  private def inputPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.in"

  private def answerPathFor(hackId: HackId): String =
    s"hacks/${hackId.value}.ans"

  private def manifestEntry(path: ProblemDataPath, bytes: Array[Byte]): ProblemDataManifestEntry =
    ProblemDataManifestEntry(path = path, sizeBytes = bytes.length.toLong, sha256 = sha256Hex(bytes))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
