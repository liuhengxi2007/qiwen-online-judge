package domains.usergroup.application

import domains.auth.model.{AuthUser, Username}
import domains.usergroup.model.{ManagedUserGroup, UserGroupDetail, UserGroupRole}

object UserGroupPolicy:

  def canCreate(actor: AuthUser): Boolean =
    val _ = actor
    true

  def canList(actor: AuthUser): Boolean =
    val _ = actor
    true

  def canView(actor: AuthUser, group: UserGroupDetail): Boolean =
    actor.siteManager || isMember(actor.username, group)

  def canEdit(actor: AuthUser, group: UserGroupDetail): Boolean =
    actor.siteManager || hasManagementRole(actor.username, group)

  def canManageMembers(actor: AuthUser, group: UserGroupDetail): Boolean =
    actor.siteManager || hasManagementRole(actor.username, group)

  def canDelete(actor: AuthUser, group: UserGroupDetail): Boolean =
    actor.siteManager || hasOwnerRole(actor.username, group)

  def requireManaged(actor: AuthUser, group: UserGroupDetail): Option[ManagedUserGroup] =
    Option.when(canEdit(actor, group))(ManagedUserGroup(group))

  private def isMember(username: Username, group: UserGroupDetail): Boolean =
    group.members.exists(_.username.value.equalsIgnoreCase(username.value))

  private def hasManagementRole(username: Username, group: UserGroupDetail): Boolean =
    group.members.exists { member =>
      member.username.value.equalsIgnoreCase(username.value) &&
      (member.role == UserGroupRole.Owner || member.role == UserGroupRole.Manager)
    }

  private def hasOwnerRole(username: Username, group: UserGroupDetail): Boolean =
    group.members.exists { member =>
      member.username.value.equalsIgnoreCase(username.value) && member.role == UserGroupRole.Owner
    }
