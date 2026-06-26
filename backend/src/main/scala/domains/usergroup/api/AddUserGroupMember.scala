package domains.usergroup.api

import cats.effect.IO
import domains.auth.api.{AuthenticatedApi, ResolveAccountUsername}
import domains.auth.objects.internal.AuthenticatedUser

import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.objects.request.AddUserGroupMemberRequest
import domains.usergroup.objects.response.UserGroupDetail
import domains.usergroup.utils.UserGroupAccessRules
import domains.usergroup.table.user_group.UserGroupTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.api.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

/** 添加用户组成员 API，要求操作者具备用户组管理权限。 */
object AddUserGroupMember extends AuthenticatedApi[(UserGroupSlug, AddUserGroupMemberRequest), UserGroupDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/user-groups/:groupSlug/members")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[UserGroupDetail] = summon[Encoder[UserGroupDetail]]

  /** 从路径解析用户组 slug，并从 JSON 请求体解码成员添加请求。 */
  override def decode(request: Request[IO], pathParams: PathParams): IO[(UserGroupSlug, AddUserGroupMemberRequest)] =
    for
      groupSlug <- HttpApiError.fromEitherBadRequest(pathParams.require("groupSlug").flatMap(UserGroupSlug.parse))
      addRequest <- request.as[AddUserGroupMemberRequest]
    yield (groupSlug, addRequest)

  /** 校验用户组可见/可管理、目标用户存在和成员唯一性，成功后返回刷新详情。 */
  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    input: (UserGroupSlug, AddUserGroupMemberRequest)
  ): IO[UserGroupDetail] =
    val (groupSlug, request) = input
    for
      validRequest <- HttpApiError.fromEitherBadRequest(UserGroupMutationValidation.validateAddMember(request))
      maybeGroup <- UserGroupTable.findBySlug(connection, groupSlug)
      group <- maybeGroup match
        case Some(group) => IO.pure(group)
        case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userGroupNotFound))
      _ <- HttpApiError.ensure(UserGroupAccessRules.canEdit(actor, group), HttpApiError.notFound(ApiMessages.userGroupNotFound))
      targetUsername <- ResolveAccountUsername.plan(connection, validRequest.username).flatMap { response =>
        response.username match
          case Some(username) => IO.pure(username)
          case None => HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
      }
      result <- UserGroupTable.addMember(connection, group.id, validRequest.copy(username = targetUsername))
      _ <- result match
        case UserGroupTable.AddMemberTableResult.AlreadyExists =>
          HttpApiError.raise(HttpApiError.conflict(ApiMessages.userAlreadyMemberOfGroup))
        case UserGroupTable.AddMemberTableResult.UserNotFound =>
          HttpApiError.raise(HttpApiError.notFound(ApiMessages.userNotFound))
        case UserGroupTable.AddMemberTableResult.Added =>
          IO.unit
      updated <- UserGroupMutationValidation.refreshBySlug(connection, group.slug, "User group disappeared after member add")
    yield UserGroupDetail.fromUserGroup(updated)
