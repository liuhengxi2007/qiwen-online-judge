package domains.auth.api

import cats.effect.IO
import domains.auth.objects.{SessionToken, SiteManagerUser}
import domains.auth.objects.internal.AuthenticatedUser
import domains.auth.table.auth_account.AuthAccountTable
import domains.auth.utils.{AuthSessionCookies, SessionStore, SessionStoreContext}
import org.http4s.Request
import shared.api.{ApiMessages, HttpApiError}

import java.sql.Connection

/** 从请求 cookie 和会话存储中解析当前用户，提供认证和站点管理员权限边界；API 对齐例外：这是后端会话基础设施，不是可调用端点文件。 */
object SessionResolver:

  /** 从请求 cookie 中读取并解析会话令牌，缺失或格式非法时返回 None。 */
  def currentSessionToken(request: Request[IO]): Option[SessionToken] =
    request.cookies.find(_.name == AuthSessionCookies.sessionCookieName).flatMap(cookie => SessionToken.parse(cookie.content).toOption)

  /** 解析当前认证用户；会访问会话存储和账号表，失败返回 401。 */
  def resolveAuthenticatedUser(
    sessionStore: SessionStoreContext,
    connection: Connection,
    request: Request[IO]
  ): IO[AuthenticatedUser] =
    currentSessionToken(request) match
      case Some(token) =>
        SessionStore.lookupUsername(sessionStore, token).flatMap {
          case Some(username) =>
            AuthAccountTable.findAuthenticatedUserByUsername(connection, username).flatMap {
              case Some(user) => IO.pure(user)
              case None => HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
            }
          case None =>
            HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))
        }
      case None =>
        HttpApiError.raise(HttpApiError.unauthorized(ApiMessages.authenticationRequired))

  /** 解析并校验当前用户为站点管理员；未登录返回 401，权限不足返回 403。 */
  def resolveSiteManager(
    sessionStore: SessionStoreContext,
    connection: Connection,
    request: Request[IO]
  ): IO[SiteManagerUser] =
    resolveAuthenticatedUser(sessionStore, connection, request).flatMap { user =>
      SiteManagerUser.from(user) match
        case Some(siteManagerUser) => IO.pure(siteManagerUser)
        case None => HttpApiError.raise(HttpApiError.forbidden(ApiMessages.siteManagerRequired))
    }
