package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.response.ResolveUserGroupSlugResponse
import domains.usergroup.table.user_group.UserGroupTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 内部用户组 slug 解析 API，供注册等流程检查 slug 冲突。 */
object ResolveUserGroupSlug extends InternalOnlyApi[UserGroupSlug, ResolveUserGroupSlugResponse]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/user-groups/resolve-slug")

  /** 按 slug 查询用户组是否存在，只返回布尔结果。 */
  override def plan(connection: Connection, slug: UserGroupSlug): IO[ResolveUserGroupSlugResponse] =
    UserGroupTable
      .findBySlug(connection, slug)
      .map(group => ResolveUserGroupSlugResponse(group.nonEmpty))
