package domains.judge.application

import cats.effect.IO
import domains.problem.application.ProblemDataStorage
import domains.problem.model.ProblemDataPath
import domains.problem.table.ProblemTable
import domains.submission.table.ClaimedSubmission
import judgeprotocol.model.{JudgeTask, JudgeTaskFileRef, JudgeTaskTestcase, ProblemSlug, ProblemSpaceLimitMb, ProblemTimeLimitMs, SubmissionId, SubmissionLanguage, SubmissionSourceCode, TestcaseName}

object JudgeTaskBuilder:

  def buildJudgeTask(
    connection: java.sql.Connection,
    claimedSubmission: ClaimedSubmission
  ): IO[Either[String, JudgeTask]] =
    for
      problem <- ProblemTable.findBySlug(connection, claimedSubmission.problemSlug)
      manifest <- ProblemDataStorage.describeManifest(claimedSubmission.problemSlug)
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
              submissionId = SubmissionId(claimedSubmission.id.value),
              problemSlug = ProblemSlug(claimedSubmission.problemSlug.value),
              language = toProtocolLanguage(claimedSubmission.language),
              sourceCode = SubmissionSourceCode(claimedSubmission.sourceCode.value),
              timeLimitMs = ProblemTimeLimitMs(claimedSubmission.timeLimitMs),
              spaceLimitMb = ProblemSpaceLimitMb(claimedSubmission.spaceLimitMb),
              problemDataVersion = manifest.version,
              testcases = testcases
            )
          )

  private def loadTestcases(claimedSubmission: ClaimedSubmission): IO[List[JudgeTaskTestcase]] =
    for
      manifest <- ProblemDataStorage.describeManifest(claimedSubmission.problemSlug)
      testcaseFiles = manifest.entries
        .filter(_.path.value.toLowerCase.endsWith(".in"))
        .sortBy(_.path.value)
      testcases <- testcaseFiles.foldLeft(IO.pure(List.empty[JudgeTaskTestcase])) { (accIO, inputEntry) =>
        for
          acc <- accIO
          maybeTestcase <- loadTestcase(inputEntry, manifest.entries)
        yield maybeTestcase match
          case Some(testcase) => acc :+ testcase
          case None => acc
      }
    yield testcases

  private def loadTestcase(
    inputEntry: domains.problem.application.ProblemDataManifestEntry,
    allEntries: List[domains.problem.application.ProblemDataManifestEntry]
  ): IO[Option[JudgeTaskTestcase]] =
    val testcaseName = inputEntry.path.fileName.stripSuffix(".in")
    val candidateOutputPaths =
      List(
        inputEntry.path.value.stripSuffix(".in") + ".out",
        inputEntry.path.value.stripSuffix(".in") + ".ans"
      ).flatMap(candidate => ProblemDataPath.parse(candidate).toOption)
    val maybeOutput = candidateOutputPaths.view.flatMap(candidate => allEntries.find(_.path == candidate)).headOption
    IO.pure(
      maybeOutput.map(outputEntry =>
        JudgeTaskTestcase(
          name = TestcaseName(testcaseName),
          input = JudgeTaskFileRef(
            path = inputEntry.path.value,
            sizeBytes = inputEntry.sizeBytes,
            sha256 = inputEntry.sha256
          ),
          expectedOutput = JudgeTaskFileRef(
            path = outputEntry.path.value,
            sizeBytes = outputEntry.sizeBytes,
            sha256 = outputEntry.sha256
          )
        )
      )
    )

  private def toProtocolLanguage(language: domains.submission.model.SubmissionLanguage): SubmissionLanguage =
    language match
      case domains.submission.model.SubmissionLanguage.Cpp17 => SubmissionLanguage.Cpp17
      case domains.submission.model.SubmissionLanguage.Python3 => SubmissionLanguage.Python3
