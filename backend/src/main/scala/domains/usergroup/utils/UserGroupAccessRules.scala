package domains.usergroup.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.user.objects.Username
import domains.usergroup.objects.{UserGroup, UserGroupRole}

/** 用户组权限规则，集中判断创建、查看、管理、删除和成员移除权限。 */
object UserGroupAccessRules:

  /** 判断用户是否可创建用户组；当前所有登录用户都允许。 */
  def canCreate(actor: AuthenticatedUser): Boolean =
    /** 注意：当前产品策略允许所有登录用户创建用户组，因此 actor 只保留作未来规则扩展入口。 */
    val _ = actor
    true

  /** 判断用户是否可访问用户组列表；当前所有登录用户都允许。 */
  def canList(actor: AuthenticatedUser): Boolean =
    /** 注意：当前产品策略允许所有登录用户访问列表，因此 actor 只保留作未来规则扩展入口。 */
    val _ = actor
    true

  /** 判断用户是否可查看用户组；站点管理员或组成员允许。 */
  def canView(actor: AuthenticatedUser, group: UserGroup): Boolean =
    actor.siteManager || isMember(actor.username, group)

  /** 判断用户是否可编辑用户组；站点管理员、owner 或 manager 允许。 */
  def canEdit(actor: AuthenticatedUser, group: UserGroup): Boolean =
    actor.siteManager || hasManagementRole(actor.username, group)

  /** 判断用户是否可删除用户组；站点管理员或 owner 允许。 */
  def canDelete(actor: AuthenticatedUser, group: UserGroup): Boolean =
    actor.siteManager || hasOwnerRole(actor.username, group)

  /** 判断用户是否可移除目标成员；manager 只能移除普通 member，owner 可移除非 owner。 */
  def canRemoveMember(actor: AuthenticatedUser, group: UserGroup, targetUsername: Username, targetRole: UserGroupRole): Boolean =
    if actor.siteManager then true
    else
      membershipRole(actor.username, group) match
        case None => false
        case Some(UserGroupRole.Owner) => targetRole != UserGroupRole.Owner
        case Some(UserGroupRole.Manager) => targetRole == UserGroupRole.Member
        case Some(UserGroupRole.Member) => false

  /** 判断用户名是否存在于当前用户组成员列表中。 */
  private def isMember(username: Username, group: UserGroup): Boolean =
    group.members.exists(_.username.value == username.value)

  /** 判断用户名是否拥有 owner 或 manager 管理角色。 */
  private def hasManagementRole(username: Username, group: UserGroup): Boolean =
    group.members.exists { member =>
      member.username.value == username.value &&
      (member.role == UserGroupRole.Owner || member.role == UserGroupRole.Manager)
    }

  /** 判断用户名是否为用户组 owner。 */
  private def hasOwnerRole(username: Username, group: UserGroup): Boolean =
    group.members.exists { member =>
      member.username.value == username.value && member.role == UserGroupRole.Owner
    }

  /** 返回用户在该组内的角色；非成员返回 None。 */
  private def membershipRole(username: Username, group: UserGroup): Option[UserGroupRole] =
    group.members.find(_.username.value == username.value).map(_.role)
