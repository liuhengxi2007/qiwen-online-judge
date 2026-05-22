package domains.usergroup.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.shared.model.{PageRequest, PageResponse}
import domains.usergroup.model.{UserGroupSlug}
import domains.usergroup.application.view.{UserGroupSummary}
import domains.usergroup.table.UserGroupTable
import domains.usergroup.application.UserGroupCommandResults.*

object UserGroupQueryCommands:

  def listUserGroups(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[UserGroupSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    if !UserGroupPolicy.canList(actor) then
      IO.pure(PageResponse(items = Nil, page = normalizedPageRequest.page, pageSize = normalizedPageRequest.pageSize, totalItems = 0L))
    else
      databaseSession.withTransactionConnection { connection =>
        UserGroupTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
      }

  def getUserGroupBySlug(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    slug: UserGroupSlug
  ): IO[GetUserGroupResult] =
    databaseSession.withTransactionConnection { connection =>
      UserGroupTable.findBySlug(connection, slug).map {
        case None => GetUserGroupResult.NotFound
        case Some(group) if !UserGroupPolicy.canView(actor, group) => GetUserGroupResult.Forbidden
        case Some(group) => GetUserGroupResult.Found(group)
      }
    }
