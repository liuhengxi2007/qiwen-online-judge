package domains.contest.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.Contest
import domains.user.objects.Username
import domains.usergroup.objects.UserGroupSlug
import shared.application.access.{ResourceAccessDecision, ResourceAccessFacts}
import shared.objects.access.{AccessUserGroupSlug, AccessUsername}

import java.time.Instant

/** 比赛访问规则工具，集中定义创建、查看、管理、赛题访问和提交边界。 */
object ContestAccessRules:

  /** 判断调用者是否拥有创建比赛的全局权限。 */
  def canCreateContests(actor: AuthenticatedUser): Boolean =
    actor.contestManager

  /** 判断调用者是否能看到比赛存在，直接可见或可管理均为 true。 */
  def canViewContest(actor: AuthenticatedUser, contest: Contest, actorGroupSlugs: Set[UserGroupSlug]): Boolean =
    val decision = evaluateContestPermissions(actor, contest, actorGroupSlugs)
    decision.canViewDirectly || decision.canManage

  /** 判断调用者是否能管理指定比赛。 */
  def canManageContest(actor: AuthenticatedUser, contest: Contest, actorGroupSlugs: Set[UserGroupSlug]): Boolean =
    evaluateContestPermissions(actor, contest, actorGroupSlugs).canManage

  /** 判断调用者是否能查看比赛详情和赛题列表，结合注册状态与当前时间窗口。 */
  def canViewContestDetail(
    actor: AuthenticatedUser,
    contest: Contest,
    actorGroupSlugs: Set[UserGroupSlug],
    isRegistered: Boolean,
    now: Instant
  ): Boolean =
    val decision = evaluateContestPermissions(actor, contest, actorGroupSlugs)
    val contestStarted = !now.isBefore(contest.startAt)
    val contestEnded = now.isAfter(contest.endAt)
    decision.canManage || (isRegistered && contestStarted) || (decision.canViewDirectly && contestEnded)

  /** 判断调用者是否能查看某个赛内题目，要求题目确实属于比赛且比赛详情可见。 */
  def canViewLinkedContestProblem(
    actor: AuthenticatedUser,
    contest: Contest,
    actorGroupSlugs: Set[UserGroupSlug],
    isRegistered: Boolean,
    containsProblem: Boolean,
    now: Instant
  ): Boolean =
    containsProblem && canViewContestDetail(actor, contest, actorGroupSlugs, isRegistered, now)

  /** 判断调用者是否能管理某个赛内题目，要求题目属于比赛且调用者可管理比赛。 */
  def canManageLinkedContestProblem(
    actor: AuthenticatedUser,
    contest: Contest,
    actorGroupSlugs: Set[UserGroupSlug],
    containsProblem: Boolean
  ): Boolean =
    containsProblem && canManageContest(actor, contest, actorGroupSlugs)

  /** 判断参赛者是否可向赛内题目提交，要求已报名、题目属于比赛且当前处于比赛时间内。 */
  def canSubmitContestProblem(
    contest: Contest,
    isRegistered: Boolean,
    containsProblem: Boolean,
    now: Instant
  ): Boolean =
    containsProblem && isRegistered && !now.isBefore(contest.startAt) && now.isBefore(contest.endAt)

  /** 使用共享资源访问规则计算比赛的直接查看和管理权限。 */
  def evaluateContestPermissions(
    actor: AuthenticatedUser,
    contest: Contest,
    actorGroupSlugs: Set[UserGroupSlug]
  ): ResourceAccessDecision =
    ResourceAccessDecision.evaluate(
      ResourceAccessFacts(
        policy = contest.accessPolicy,
        actorUsername = toAccessUsername(actor.username),
        actorGroupSlugs = toAccessGroupSlugs(actorGroupSlugs),
        hasGlobalViewOverride = hasGlobalOverride(actor),
        hasGlobalManageOverride = hasGlobalOverride(actor)
      )
    )

  private def hasGlobalOverride(actor: AuthenticatedUser): Boolean =
    actor.contestManager

  private def toAccessUsername(username: Username): AccessUsername =
    AccessUsername(username.value)

  private def toAccessGroupSlugs(slugs: Set[UserGroupSlug]): Set[AccessUserGroupSlug] =
    slugs.map(slug => AccessUserGroupSlug(slug.value))
