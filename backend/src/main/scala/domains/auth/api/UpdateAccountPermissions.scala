package domains.auth.api

import cats.effect.IO
import domains.auth.objects.SiteManagerUser
import domains.auth.objects.request.UpdateUserPermissionsRequest
import domains.auth.objects.response.AuthAccountListItem
import domains.auth.utils.AuthAccountRules
import domains.auth.table.auth_account.AuthAccountTable
import domains.user.api.FindUserProfileSettings
import domains.user.objects.Username
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 站点管理员权限更新 API，用于调整用户的站点/题目/比赛管理权限。 */
object UpdateAccountPermissions extends SiteManagerApi[(Username, UpdateUserPermissionsRequest), AuthAccountListItem]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/auth/accounts/:targetUsername/permissions")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[AuthAccountListItem] = summon[Encoder[AuthAccountListItem]]

  /** 从路径读取目标用户名并从 JSON 请求体解码权限变更。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(Username, UpdateUserPermissionsRequest)] =
    for
      rawUsername <- HttpApiError.fromEitherBadRequest(pathParams.require("targetUsername"))
      updateRequest <- request.as[UpdateUserPermissionsRequest]
    yield (Username.canonical(rawUsername), updateRequest)

  /** 保护内置 admin 权限不可变，更新后返回带资料的管理列表项。 */
  override def plan(
    connection: Connection,
    actor: SiteManagerUser,
    input: (Username, UpdateUserPermissionsRequest)
  ): IO[AuthAccountListItem] =
    val (targetUsername, request) = input
    for
      _ <- HttpApiError.ensure(
        targetUsername.value != AuthAccountRules.protectedAdminUsername,
        HttpApiError.forbidden(ApiMessages.adminPermissionsImmutable)
      )
      updated <- AuthAccountTable.updatePermissions(
        connection,
        actor,
        targetUsername,
        siteManager = request.siteManager,
        problemManager = request.problemManager,
        contestManager = request.contestManager
      )
      user <- updated match
        case Some(user) => IO.pure(user)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      profile <- FindUserProfileSettings.plan(connection, user.username).flatMap {
        case Some(profile) => IO.pure(profile)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
    yield AuthAccountListItem.fromParts(user, profile)
