package domains.usergroup.rules

import domains.auth.objects.AuthUser
import domains.user.objects.Username
import domains.usergroup.objects.{UserGroup, UserGroupRole}

object UserGroupAccessRules:

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

  def canDelete(actor: AuthUser, group: UserGroup): Boolean =
    actor.siteManager || hasOwnerRole(actor.username, group)

  def canRemoveMember(actor: AuthUser, group: UserGroup, targetUsername: Username, targetRole: UserGroupRole): Boolean =
    if actor.siteManager then true
    else
      membershipRole(actor.username, group) match
        case None => false
        case Some(UserGroupRole.Owner) => targetRole != UserGroupRole.Owner
        case Some(UserGroupRole.Manager) => targetRole == UserGroupRole.Member
        case Some(UserGroupRole.Member) => false

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

  private def membershipRole(username: Username, group: UserGroup): Option[UserGroupRole] =
    group.members.find(_.username.value == username.value).map(_.role)
