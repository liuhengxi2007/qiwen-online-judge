package domains.judge.application

import cats.effect.IO
import database.DatabaseSession
import domains.judger.application.JudgerRegistryCommands
import domains.problem.application.ProblemDataStorage
import domains.problem.objects.response.ProblemDetail
import domains.problem.objects.ProblemDataPath
import domains.problem.objects.internal.ProblemDataManifest
import domains.submission.application.SubmissionCommands
import domains.submission.objects.SubmissionId
import judgeprotocol.objects.{JudgeTask, JudgerId, ReportJudgeResultRequest}

import java.time.Instant

object JudgeCommands:

  enum ClaimJudgeTaskResult:
    case NoTask
    case ValidationFailed(message: String)
    case Claimed(task: JudgeTask)

  enum ReportJudgeResult:
    case ValidationFailed(message: String)
    case SubmissionNotFound
    case Updated

  def validateProblemReadyConfig(
    bytes: Array[Byte],
    problem: ProblemDetail,
    manifest: ProblemDataManifest
  ): Either[String, Set[ProblemDataPath]] =
    JudgeTaskBuilder
      .validateReadyConfigBytes(bytes, problem, manifest)
      .map(_.retainedPaths)

  def claimTask(
    databaseSession: DatabaseSession,
    judgeConfig: JudgeConfig,
    problemDataStorage: ProblemDataStorage,
    judgerId: JudgerId,
    claimedAt: Instant
  ): IO[ClaimJudgeTaskResult] =
    databaseSession.withTransactionConnection { connection =>
      JudgerRegistryCommands.resolveActiveSupportedLanguages(connection, judgeConfig, judgerId).flatMap {
        case None =>
          IO.pure(ClaimJudgeTaskResult.ValidationFailed(s"Judger ${judgerId.value} is not registered or its lease expired."))
        case Some(supportedLanguages) =>
          SubmissionCommands.claimNextJudgeTask(connection, supportedLanguages, claimedAt).flatMap {
            case SubmissionCommands.ClaimNextJudgeTaskResult.ValidationFailed(message) =>
              IO.pure(ClaimJudgeTaskResult.ValidationFailed(message))
            case SubmissionCommands.ClaimNextJudgeTaskResult.NoTask =>
              IO.pure(ClaimJudgeTaskResult.NoTask)
            case SubmissionCommands.ClaimNextJudgeTaskResult.Claimed(claimedSubmission) =>
              JudgeTaskBuilder.buildJudgeTask(connection, problemDataStorage, claimedSubmission).flatMap {
                case Left(message) =>
                  SubmissionCommands
                    .failClaimedJudgeTask(connection, claimedSubmission, judgerId, claimedAt, message)
                    .map {
                      case Left(lifecycleMessage) => ClaimJudgeTaskResult.ValidationFailed(lifecycleMessage)
                      case Right(_) => ClaimJudgeTaskResult.ValidationFailed(message)
                    }
                case Right(task) =>
                  IO.pure(ClaimJudgeTaskResult.Claimed(task))
              }
          }
      }
    }

  def reportJudgeResult(
    databaseSession: DatabaseSession,
    submissionId: SubmissionId,
    request: ReportJudgeResultRequest,
    completedAt: Instant
  ): IO[ReportJudgeResult] =
    databaseSession.withTransactionConnection { connection =>
      SubmissionCommands.recordJudgeResult(connection, submissionId, request, completedAt).map {
        case SubmissionCommands.RecordJudgeResult.ValidationFailed(message) => ReportJudgeResult.ValidationFailed(message)
        case SubmissionCommands.RecordJudgeResult.SubmissionNotFound => ReportJudgeResult.SubmissionNotFound
        case SubmissionCommands.RecordJudgeResult.Updated => ReportJudgeResult.Updated
      }
    }
