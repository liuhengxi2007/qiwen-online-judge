package domains.usergroup.objects.request

import domains.usergroup.objects.*

final case class CreateUserGroupRequest(
  slug: UserGroupSlug,
  name: UserGroupName,
  description: UserGroupDescription
)
