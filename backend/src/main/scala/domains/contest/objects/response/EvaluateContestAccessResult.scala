package domains.contest.objects.response

import domains.contest.objects.{ContestId, ContestSlug, ContestTitle}

/** 内部权限评估输出，供提交、题目详情等跨 domain 流程复用，不直接产生 HTTP 错误。 */
final case class EvaluateContestAccessResult(
  contestId: ContestId,
  contestSlug: ContestSlug,
  contestTitle: ContestTitle,
  contestStarted: Boolean,
  contestEnded: Boolean,
  isRegistered: Boolean,
  containsProblem: Boolean,
  canViewContest: Boolean,
  canViewContestDetail: Boolean,
  canManageContest: Boolean,
  canViewLinkedContestProblem: Boolean,
  canManageLinkedContestProblem: Boolean,
  canSubmitContestProblem: Boolean
)
