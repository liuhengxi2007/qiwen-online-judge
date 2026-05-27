package shared.objects.access

final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)
