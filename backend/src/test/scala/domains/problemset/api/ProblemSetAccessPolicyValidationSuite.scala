package domains.problemset.api

import munit.CatsEffectSuite
import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, BaseAccess, ResourceVisibilityPolicy}

class ProblemSetAccessPolicyValidationSuite extends CatsEffectSuite:

  test("sanitizePolicy deduplicates viewer grants") {
    val policy = ResourceVisibilityPolicy(
      BaseAccess.Restricted,
      viewerGrants = List(
        AccessSubject.UserGroup(AccessUserGroupSlug("reviewers")),
        AccessSubject.User(AccessUsername("alice")),
        AccessSubject.User(AccessUsername("alice")),
        AccessSubject.UserGroup(AccessUserGroupSlug("reviewers"))
      )
    )

    assertEquals(
      ProblemSetAccessPolicyValidation.sanitizePolicy(policy).viewerGrants,
      List(
        AccessSubject.UserGroup(AccessUserGroupSlug("reviewers")),
        AccessSubject.User(AccessUsername("alice"))
      )
    )
  }
