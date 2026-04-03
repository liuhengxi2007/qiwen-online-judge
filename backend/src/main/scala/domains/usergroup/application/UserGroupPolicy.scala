package domains.usergroup.application

import domains.auth.model.{AuthUser, Username}
import domains.usergroup.model.{ManagedUserGroup, OwnedUserGroup, UserGroup, UserGroupRole}

object UserGroupPolicy:

  def canCreate(actor: AuthUser): Boolean =
    val _ = actor
    true

  def canList(actor: AuthUser): Boolean =
    val _ = actor
    true

  def canView(actor: AuthUser, group: UserGroup): Boolean =
    actor.siteManager || isMember(actor.username, group)

  def canEdit(actor: AuthUser, group: UserGroup): Boolean =
    actor.siteManager || hasManagementRole(actor.username, group)

  def canManageMembers(actor: AuthUser, group: UserGroup): Boolean =
    actor.siteManager || hasManagementRole(actor.username, group)

  def canDelete(actor: AuthUser, group: UserGroup): Boolean =
    actor.siteManager || hasOwnerRole(actor.username, group)

  def requireManaged(actor: AuthUser, group: UserGroup): Option[ManagedUserGroup] =
    Option.when(canEdit(actor, group))(ManagedUserGroup(group))

  def requireOwned(actor: AuthUser, group: UserGroup): Option[OwnedUserGroup] =
    Option.when(canDelete(actor, group))(OwnedUserGroup(group))

  private def isMember(username: Username, group: UserGroup): Boolean =
    group.members.exists(_.username.value == username.value)

  private def hasManagementRole(username: Username, group: UserGroup): Boolean =
    group.members.exists { member =>
      member.username.value == username.value &&
      (member.role == UserGroupRole.Owner || member.role == UserGroupRole.Manager)
    }

  private def hasOwnerRole(username: Username, group: UserGroup): Boolean =
    group.members.exists { member =>
      member.username.value == username.value && member.role == UserGroupRole.Owner
    }
