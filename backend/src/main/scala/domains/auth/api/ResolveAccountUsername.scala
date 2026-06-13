package domains.auth.api

import cats.effect.IO
import domains.auth.objects.response.ResolveAccountUsernameResponse
import domains.auth.table.auth_account.AuthAccountTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部账号解析 API，供其他领域按用户名检查账号存在并获得规范用户名。 */
object ResolveAccountUsername extends InternalOnlyApi[Username, ResolveAccountUsernameResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/auth/resolve-account-username")

  /** 在账号表中按大小写不敏感用户名查找账号，返回可选规范用户名。 */
  override def plan(connection: Connection, username: Username): IO[ResolveAccountUsernameResponse] =
    AuthAccountTable
      .findAccountByUsername(connection, username)
      .map(user => ResolveAccountUsernameResponse(user.map(_.username)))
