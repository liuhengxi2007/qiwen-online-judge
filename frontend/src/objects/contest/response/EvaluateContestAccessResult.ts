import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'

/** 内部比赛访问评估结果；镜像后端 EvaluateContestAccess.Result。 */
export type EvaluateContestAccessResult = {
  contestId: ContestId
  contestSlug: ContestSlug
  contestTitle: ContestTitle
  contestStarted: boolean
  contestEnded: boolean
  isRegistered: boolean
  containsProblem: boolean
  canViewContest: boolean
  canViewContestDetail: boolean
  canManageContest: boolean
  canViewLinkedContestProblem: boolean
  canManageLinkedContestProblem: boolean
  canSubmitContestProblem: boolean
}
