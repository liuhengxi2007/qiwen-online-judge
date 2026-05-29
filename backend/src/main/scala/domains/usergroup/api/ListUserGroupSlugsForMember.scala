package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.user.objects.Username
import domains.usergroup.objects.response.UserGroupSlugListResponse
import domains.usergroup.table.user_group.UserGroupTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

object ListUserGroupSlugsForMember extends InternalOnlyApi[Username, UserGroupSlugListResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/user-groups/member-slugs")

  override def plan(connection: Connection, username: Username): IO[UserGroupSlugListResponse] =
    UserGroupTable
      .listGroupSlugsForMember(connection, username)
      .map(slugs => UserGroupSlugListResponse(slugs.toList.sortBy(_.value)))
