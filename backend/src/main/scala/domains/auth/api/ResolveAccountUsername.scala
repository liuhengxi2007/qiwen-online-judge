package domains.auth.api

import cats.effect.IO
import domains.auth.objects.response.ResolveAccountUsernameResponse
import domains.auth.table.auth_user.AuthUserTable
import domains.user.objects.Username
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object ResolveAccountUsername extends InternalOnlyApi[Username, ResolveAccountUsernameResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/auth/resolve-account-username")

  override def plan(connection: Connection, username: Username): IO[ResolveAccountUsernameResponse] =
    AuthUserTable
      .findByUsername(connection, username)
      .map(user => ResolveAccountUsernameResponse(user.map(_.username)))
