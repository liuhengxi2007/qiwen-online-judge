package domains.auth.http.api

import cats.effect.IO
import domains.auth.http.{AuthApiSupport, SiteManagerApi}
import domains.auth.http.codec.AuthHttpCodecs.given
import domains.auth.objects.SiteManagerUser
import domains.auth.objects.request.UpdateUserPermissionsRequest
import domains.auth.objects.response.AuthAccountListItem
import domains.auth.table.auth_user.AuthUserTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object UpdateAccountPermissions extends SiteManagerApi[(Username, UpdateUserPermissionsRequest), AuthAccountListItem]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/accounts/:targetUsername/permissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[AuthAccountListItem] = summon[Encoder[AuthAccountListItem]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, UpdateUserPermissionsRequest)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      updateRequest <- request.as[UpdateUserPermissionsRequest]
    yield (Username.canonical(rawUsername), updateRequest)

  override def plan(
    connection: Connection,
    actor: SiteManagerUser,
    input: (Username, UpdateUserPermissionsRequest)
  ): IO[AuthAccountListItem] =
    val (targetUsername, request) = input
    for
      _ <- HttpApiError.ensure(
        targetUsername.value != AuthApiSupport.protectedAdminUsername,
        HttpApiError.forbidden(ApiMessages.adminPermissionsImmutable)
      )
      updated <- AuthUserTable.updatePermissions(
        connection,
        actor,
        targetUsername,
        siteManager = request.siteManager,
        problemManager = request.problemManager
      )
      user <- updated match
        case Some(user) => IO.pure(user)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
    yield AuthApiSupport.toAuthAccountListItem(user)
