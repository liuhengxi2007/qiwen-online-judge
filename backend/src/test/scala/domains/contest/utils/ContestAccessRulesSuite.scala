package domains.contest.utils

import domains.auth.objects.AuthPermissionFlags
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.{Contest, ContestDescription, ContestId, ContestSlug, ContestTitle}
import domains.user.objects.Username
import munit.FunSuite
import shared.objects.access.{BaseAccess, ResourceAccessPolicy}

import java.time.Instant
import java.util.UUID

class ContestAccessRulesSuite extends FunSuite:

  private val contest = Contest(
    id = ContestId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
    slug = ContestSlug("contest-1"),
    title = ContestTitle("Contest 1"),
    description = ContestDescription(""),
    startAt = Instant.parse("2026-01-01T00:00:00Z"),
    endAt = Instant.parse("2026-01-01T01:00:00Z"),
    problems = Nil,
    accessPolicy = ResourceAccessPolicy(BaseAccess.Restricted, viewerGrants = Nil, managerGrants = Nil),
    author = None,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-01-01T00:00:00Z")
  )

  test("normalized site manager can create and manage contests") {
    val actor = authenticatedUser("admin", siteManager = true, problemManager = false, contestManager = false)

    assert(ContestAccessRules.canCreateContests(actor))
    assert(ContestAccessRules.canManageContest(actor, contest, Set.empty))
  }

  test("non-site non-contest manager cannot create contests") {
    val actor = authenticatedUser("alice", siteManager = false, problemManager = true, contestManager = false)

    assert(!ContestAccessRules.canCreateContests(actor))
  }

  test("detail visibility follows manager participant started and viewer ended matrix") {
    val actor = authenticatedUser("alice", siteManager = false, problemManager = false, contestManager = false)
    val manager = authenticatedUser("manager", siteManager = false, problemManager = false, contestManager = true)
    val publicContest = contest.copy(accessPolicy = ResourceAccessPolicy(BaseAccess.Public, viewerGrants = Nil, managerGrants = Nil))
    val beforeStart = Instant.parse("2025-12-31T23:59:00Z")
    val duringContest = Instant.parse("2026-01-01T00:30:00Z")
    val afterEnd = Instant.parse("2026-01-01T01:01:00Z")

    assert(!ContestAccessRules.canViewContestDetail(actor, publicContest, Set.empty, isRegistered = false, beforeStart))
    assert(ContestAccessRules.canViewContestDetail(actor, contest, Set.empty, isRegistered = true, duringContest))
    assert(ContestAccessRules.canViewContestDetail(actor, publicContest, Set.empty, isRegistered = false, afterEnd))
    assert(ContestAccessRules.canViewContestDetail(manager, contest, Set.empty, isRegistered = false, beforeStart))
  }

  test("linked contest problem permissions require relation and contest-scoped access") {
    val actor = authenticatedUser("alice", siteManager = false, problemManager = false, contestManager = false)
    val manager = authenticatedUser("manager", siteManager = false, problemManager = false, contestManager = true)
    val duringContest = Instant.parse("2026-01-01T00:30:00Z")

    assert(ContestAccessRules.canViewLinkedContestProblem(actor, contest, Set.empty, isRegistered = true, containsProblem = true, duringContest))
    assert(!ContestAccessRules.canViewLinkedContestProblem(actor, contest, Set.empty, isRegistered = true, containsProblem = false, duringContest))
    assert(ContestAccessRules.canManageLinkedContestProblem(manager, contest, Set.empty, containsProblem = true))
    assert(!ContestAccessRules.canManageLinkedContestProblem(manager, contest, Set.empty, containsProblem = false))
  }

  test("contest problem submission requires linked active contest registration") {
    val beforeStart = Instant.parse("2025-12-31T23:59:00Z")
    val duringContest = Instant.parse("2026-01-01T00:30:00Z")
    val afterEnd = Instant.parse("2026-01-01T01:01:00Z")

    assert(ContestAccessRules.canSubmitContestProblem(contest, isRegistered = true, containsProblem = true, duringContest))
    assert(!ContestAccessRules.canSubmitContestProblem(contest, isRegistered = false, containsProblem = true, duringContest))
    assert(!ContestAccessRules.canSubmitContestProblem(contest, isRegistered = true, containsProblem = false, duringContest))
    assert(!ContestAccessRules.canSubmitContestProblem(contest, isRegistered = true, containsProblem = true, beforeStart))
    assert(!ContestAccessRules.canSubmitContestProblem(contest, isRegistered = true, containsProblem = true, afterEnd))
  }

  private def authenticatedUser(
    username: String,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean
  ): AuthenticatedUser =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    AuthenticatedUser(
      username = Username.canonical(username),
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
