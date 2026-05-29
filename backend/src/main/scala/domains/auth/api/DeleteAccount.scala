package domains.auth.api

import cats.effect.IO
import domains.auth.objects.SiteManagerUser
import domains.auth.utils.AuthAccountRules
import domains.auth.table.auth_user.AuthUserTable
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}
import shared.objects.response.SuccessResponse

import java.sql.Connection

object DeleteAccount extends SiteManagerApi[Username, SuccessResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/accounts/:targetUsername/delete")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[SuccessResponse] = summon[Encoder[SuccessResponse]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[Username] =
    val _ = request
    pathParams.require("targetUsername") match
      case Right(rawUsername) => IO.pure(Username.canonical(rawUsername))
      case Left(message) => HttpApiError.raise(HttpApiError.badRequest(message))

  override def plan(connection: Connection, actor: SiteManagerUser, targetUsername: Username): IO[SuccessResponse] =
    for
      _ <- HttpApiError.ensure(
        targetUsername.value != AuthAccountRules.protectedAdminUsername,
        HttpApiError.forbidden(ApiMessages.adminDeleteForbidden)
      )
      _ <- HttpApiError.ensure(
        targetUsername.value != actor.authUser.username.value,
        HttpApiError.badRequest(ApiMessages.cannotDeleteSelf)
      )
      deleteResult <- AuthUserTable.delete(connection, targetUsername)
      _ <- deleteResult match
        case AuthUserTable.DeleteAccountTableResult.NotFound =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
        case AuthUserTable.DeleteAccountTableResult.HasOwnedResources =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.userHasOwnedResources))
        case AuthUserTable.DeleteAccountTableResult.Deleted =>
          IO.unit
    yield SuccessResponse.fromApiMessage(ApiMessages.userDeleted)
