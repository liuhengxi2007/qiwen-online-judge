package shared.objects.access

import java.time.Instant

final case class ResourceAccessGrant(
  resourceKind: ResourceKind,
  resourceId: ResourceId,
  grantRole: GrantRole,
  subject: AccessSubject,
  createdAt: Instant
)
