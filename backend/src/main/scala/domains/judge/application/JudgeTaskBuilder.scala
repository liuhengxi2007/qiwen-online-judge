package domains.judge.application

import cats.effect.IO
import domains.judge.model.{JudgeTask, JudgeTaskTestcase}
import domains.problem.application.ProblemDataStorage
import domains.problem.model.ProblemDataFilename
import domains.problem.table.ProblemTable
import domains.submission.table.ClaimedSubmission

import java.util.Base64

object JudgeTaskBuilder:

  def buildJudgeTask(
    connection: java.sql.Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[Either[String, JudgeTask]] =
    for
      problem <- ProblemTable.findBySlug(connection, claimedSubmission.problemSlug)
      testcases <- loadTestcases(claimedSubmission)
    yield
      problem match
        case None =>
          Left("Problem not found for claimed submission.")
        case Some(_) if testcases.isEmpty =>
          Left("No valid testcases were found for this problem.")
        case Some(_) =>
          Right(
            JudgeTask(
              submissionId = claimedSubmission.id,
              problemSlug = claimedSubmission.problemSlug,
              language = claimedSubmission.language,
              sourceCode = claimedSubmission.sourceCode.value,
              timeLimitMs = claimedSubmission.timeLimitMs,
              spaceLimitMb = claimedSubmission.spaceLimitMb,
              testcases = testcases
            )
          )

  private def loadTestcases(claimedSubmission: ClaimedSubmission): IO[List[JudgeTaskTestcase]] =
    for
      files <- ProblemDataStorage.listFiles(claimedSubmission.problemSlug)
      testcaseFiles = files
        .filter(_.value.toLowerCase.endsWith(".in"))
        .sortBy(_.value)
      testcases <- testcaseFiles.foldLeft(IO.pure(List.empty[JudgeTaskTestcase])) { (accIO, inputFilename) =>
        for
          acc <- accIO
          maybeTestcase <- loadTestcase(claimedSubmission, inputFilename)
        yield maybeTestcase match
          case Some(testcase) => acc :+ testcase
          case None => acc
      }
    yield testcases

  private def loadTestcase(
    claimedSubmission: ClaimedSubmission,
    inputFilename: ProblemDataFilename
  ): IO[Option[JudgeTaskTestcase]] =
    val testcaseName = inputFilename.value.stripSuffix(".in")
    val candidateOutputFilenames = List(
      ProblemDataFilename.unsafe(testcaseName + ".out"),
      ProblemDataFilename.unsafe(testcaseName + ".ans")
    )
    for
      maybeInput <- ProblemDataStorage.readFile(claimedSubmission.problemSlug, inputFilename)
      maybeOutput <- loadExpectedOutput(claimedSubmission, candidateOutputFilenames)
    yield
      for
        (_, inputBytes) <- maybeInput
        (_, outputBytes) <- maybeOutput
      yield JudgeTaskTestcase(
        name = testcaseName,
        inputBase64 = Base64.getEncoder.encodeToString(inputBytes),
        expectedOutputBase64 = Base64.getEncoder.encodeToString(outputBytes)
      )

  private def loadExpectedOutput(
    claimedSubmission: ClaimedSubmission,
    candidateOutputFilenames: List[ProblemDataFilename]
  ): IO[Option[(ProblemDataFilename, Array[Byte])]] =
    candidateOutputFilenames match
      case Nil =>
        IO.pure(None)
      case filename :: remaining =>
        ProblemDataStorage.readFile(claimedSubmission.problemSlug, filename).flatMap {
          case some @ Some(_) => IO.pure(some)
          case None => loadExpectedOutput(claimedSubmission, remaining)
        }
