package domains.submission.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.table.contest.ContestProblemVisibilityTable
import domains.problem.api.EvaluateProblemAccess
import domains.submission.utils.SubmissionAccessRules

import domains.submission.objects.SubmissionId
import domains.submission.objects.internal.SubmissionDetailRecord
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.SubmissionQueryTable
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

final case class GetSubmission(submissionProgramStorage: SubmissionProgramStorage) extends AuthenticatedApi[SubmissionId, SubmissionDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  override def plan(connection: Connection, actor: AuthenticatedUser, submissionId: SubmissionId): IO[SubmissionDetail] =
    SubmissionQueryTable.findById(connection, submissionId).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      case Some(record) =>
        EvaluateProblemAccess.plan(connection, actor, record.problemSlug).flatMap { access =>
          access.problem match
            case None =>
              HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
            case Some(problem) =>
              for
                hasVisibleUnfinishedContestContainingProblem <- ContestProblemVisibilityTable
                  .hasVisibleUnfinishedContestContainingProblem(connection, actor, record.problemId)
                hasVisibleEndedContestContainingProblem <- ContestProblemVisibilityTable
                  .hasVisibleEndedContestContainingProblem(connection, actor, record.problemId)
                isOwnRegisteredContestSubmission <- ContestProblemVisibilityTable
                  .hasRegisteredContestContainingSubmission(connection, actor, record.problemId, record.submittedAt)
                submission <-
                  if access.canManage then
                    loadSubmissionDetail(record, access.canManage)
                  else if hasVisibleUnfinishedContestContainingProblem then
                    for
                      _ <- HttpApiError.ensure(
                        record.submitter.username == actor.username && isOwnRegisteredContestSubmission,
                        HttpApiError.notFound(ApiMessages.submissionNotFound)
                      )
                      submission <- loadSubmissionDetail(record, access.canManage)
                    yield submission
                  else if hasVisibleEndedContestContainingProblem then
                    loadSubmissionDetail(record, access.canManage)
                  else if SubmissionAccessRules.canViewOwnOrWithGlobalOverride(actor, record.submitter.username) then
                    loadSubmissionDetail(record, access.canManage)
                  else
                    for
                      _ <- HttpApiError.ensure(access.canView, HttpApiError.notFound(ApiMessages.submissionNotFound))
                      _ <- HttpApiError.ensure(
                        SubmissionAccessRules.canViewDetailOfOthers(problem.otherUserSubmissionAccess),
                        HttpApiError.notFound(ApiMessages.submissionNotFound)
                      )
                      submission <- loadSubmissionDetail(record, access.canManage)
                    yield submission
              yield submission
        }
    }

  private def loadSubmissionDetail(record: SubmissionDetailRecord, canManage: Boolean): IO[SubmissionDetail] =
    submissionProgramStorage.readDefaultSource(record.programManifest).flatMap {
      case Right(sourceCode) => IO.pure(SubmissionDetail.fromRecord(record, sourceCode, canManage))
      case Left(message) => HttpApiError.raise(HttpApiError.internal(message))
    }
