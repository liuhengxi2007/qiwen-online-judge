package domains.contest.utils

import domains.contest.objects.{ContestDescription, ContestSlug, ContestTitle}
import domains.contest.objects.request.{CreateContestRequest, UpdateContestRequest}
import munit.FunSuite
import shared.objects.access.{AccessSubject, AccessUserGroupSlug, AccessUsername, BaseAccess, ResourceAccessPolicy}

import java.time.Instant

class ContestAccessPolicyValidationSuite extends FunSuite:

  private val startAt = Instant.parse("2026-01-01T00:00:00Z")
  private val endAt = Instant.parse("2026-01-01T01:00:00Z")
  private val managerGrant = AccessSubject.User(AccessUsername("manager-1"))
  private val groupManagerGrant = AccessSubject.UserGroup(AccessUserGroupSlug("group-1"))

  test("create sanitize does not inject author manager grants") {
    val request = CreateContestRequest(
      slug = ContestSlug("contest-1"),
      title = ContestTitle("Contest 1"),
      description = ContestDescription(""),
      startAt = startAt,
      endAt = endAt,
      accessPolicy = ResourceAccessPolicy(BaseAccess.Restricted, viewerGrants = Nil, managerGrants = Nil)
    )

    assertEquals(ContestAccessPolicyValidation.sanitizePolicy(request).accessPolicy.managerGrants, Nil)
  }

  test("update sanitize does not inject author manager grants") {
    val request = UpdateContestRequest(
      title = ContestTitle("Contest 1"),
      description = ContestDescription(""),
      startAt = startAt,
      endAt = endAt,
      accessPolicy = ResourceAccessPolicy(BaseAccess.Restricted, viewerGrants = Nil, managerGrants = Nil)
    )

    assertEquals(ContestAccessPolicyValidation.sanitizePolicy(request).accessPolicy.managerGrants, Nil)
  }

  test("sanitize keeps explicit grants and deduplicates them") {
    val policy = ResourceAccessPolicy(
      BaseAccess.Restricted,
      viewerGrants = Nil,
      managerGrants = List(managerGrant, managerGrant, groupManagerGrant)
    )

    assertEquals(
      ContestAccessPolicyValidation.sanitizePolicy(policy).managerGrants,
      List(managerGrant, groupManagerGrant)
    )
  }
