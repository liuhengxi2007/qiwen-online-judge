package shared.access

final case class ResourceAccessPolicy(
  baseAccess: BaseAccess,
  viewerGrants: List[AccessSubject],
  managerGrants: List[AccessSubject]
)
