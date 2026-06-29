package shared.application.access

import munit.FunSuite
import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, BaseAccess, ResourceAccessPolicy}

class AccessPolicyEvaluatorSuite extends FunSuite:

  test("restricted access does not grant view without an explicit grant or global override") {
    val policy = ResourceAccessPolicy(BaseAccess.Restricted, viewerGrants = Nil, managerGrants = Nil)

    assertEquals(
      AccessPolicyEvaluator.canView(
        policy = policy,
        viewerUsername = AccessUsername("creator"),
        viewerGroupSlugs = Set.empty,
        hasGlobalOverride = false
      ),
      false
    )
  }

  test("restricted access grants view to explicit viewer subjects") {
    val policy = ResourceAccessPolicy(
      BaseAccess.Restricted,
      viewerGrants = List(AccessSubject.UserGroup(AccessUserGroupSlug("reviewers"))),
      managerGrants = Nil
    )

    assertEquals(
      AccessPolicyEvaluator.canView(
        policy = policy,
        viewerUsername = AccessUsername("alice"),
        viewerGroupSlugs = Set(AccessUserGroupSlug("reviewers")),
        hasGlobalOverride = false
      ),
      true
    )
  }
