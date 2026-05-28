package domains.submission.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.submission.http.SubmissionApiSupport
import domains.submission.http.codec.SubmissionHttpCodecs.given
import domains.submission.objects.request.SubmissionListRequest
import domains.submission.objects.response.SubmissionListResponse
import domains.submission.table.submission.SubmissionQueryTable
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.http.{ApiPath, PathParams}

import java.sql.Connection

object ListSubmissions extends AuthenticatedApi[SubmissionListRequest, SubmissionListResponse]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/submissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SubmissionListResponse] = summon[Encoder[SubmissionListResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[SubmissionListRequest] =
    val _ = pathParams
    IO.pure(SubmissionApiSupport.listSubmissionsRequest(request.uri.query.params))

  override def plan(connection: Connection, actor: AuthUser, request: SubmissionListRequest): IO[SubmissionListResponse] =
    SubmissionQueryTable.listVisibleTo(connection, actor, request, SubmissionApiSupport.hasGlobalViewOverride(actor))
