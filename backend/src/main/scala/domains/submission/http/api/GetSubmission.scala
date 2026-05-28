package domains.submission.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problem.rules.ProblemAccessRules
import domains.problem.table.problem.ProblemQueryTable
import domains.submission.http.SubmissionApiSupport
import domains.submission.http.codec.SubmissionHttpCodecs.given
import domains.submission.objects.SubmissionId
import domains.submission.objects.response.SubmissionDetail
import domains.submission.table.submission.SubmissionQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object GetSubmission extends AuthenticatedApi[SubmissionId, SubmissionDetail]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions/:submissionId")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionDetail] = summon[Encoder[SubmissionDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionId] =
    val _ = request
    HttpApiError.fromEitherBadRequest(pathParams.require("submissionId").flatMap(SubmissionId.parse))

  override def plan(connection: Connection, actor: AuthUser, submissionId: SubmissionId): IO[SubmissionDetail] =
    SubmissionQueryTable.findById(connection, submissionId).flatMap {
      case None =>
        HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
      case Some(submission) =>
        ProblemQueryTable.findBySlug(connection, submission.problemSlug).flatMap {
          case None =>
            HttpApiError.raise(HttpApiError.notFound(ApiMessages.submissionNotFound))
          case Some(problem) =>
            ProblemAccessRules.evaluateProblemPermissions(connection, actor, problem).flatMap { access =>
              if SubmissionApiSupport.canViewOwnOrWithGlobalOverride(actor, submission.submitter.username) then
                IO.pure(submission.copy(canManage = access.canManage))
              else
                for
                  _ <- HttpApiError.ensure(access.canView, HttpApiError.notFound(ApiMessages.submissionNotFound))
                  _ <- HttpApiError.ensure(
                    SubmissionApiSupport.canViewDetailOfOthers(problem.othersSubmissionAccess),
                    HttpApiError.notFound(ApiMessages.submissionNotFound)
                  )
                yield submission.copy(canManage = access.canManage)
            }
        }
    }
