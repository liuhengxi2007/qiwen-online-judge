package domains.usergroup.objects.internal

import domains.usergroup.objects.UserGroup

/** 已确认当前操作者拥有所有者权限的用户组包装，供领域内部表达权限收窄。 */
final case class OwnedUserGroup private[usergroup] (value: UserGroup)
