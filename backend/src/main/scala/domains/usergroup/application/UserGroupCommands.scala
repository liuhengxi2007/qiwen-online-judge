package domains.usergroup.application



import cats.effect.IO
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import domains.usergroup.table.user_group.UserGroupTable

import java.sql.Connection

object UserGroupCommands:
  export UserGroupCommandResults.*
  export UserGroupQueryCommands.*
  export UserGroupMutationCommands.*
  export UserGroupMemberCommands.*

  def accessActorGroupSlugs(connection: Connection, username: Username): IO[Set[UserGroupSlug]] =
    UserGroupTable.listGroupSlugsForMember(connection, username)

  def accessPolicyUserGroupExists(connection: Connection, slug: UserGroupSlug): IO[Boolean] =
    UserGroupTable.findBySlug(connection, slug).map(_.nonEmpty)

  def userGroupSlugConflictsWith(connection: Connection, rawValue: String): IO[Boolean] =
    UserGroupSlug.parse(rawValue) match
      case Left(_) => IO.pure(false)
      case Right(slug) => UserGroupTable.findBySlug(connection, slug).map(_.nonEmpty)
