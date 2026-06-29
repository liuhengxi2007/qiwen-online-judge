import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { EvaluateContestAccessResult } from '@/objects/contest/response/EvaluateContestAccessResult'
import { parseContestTitle } from '@/objects/contest/ContestTitle'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ContestId } from '@/objects/contest/ContestId'

/** 内部比赛访问评估请求体；problemId 为空表示只评估比赛本身访问。 */
type EvaluateContestAccessBody = {
  contestSlug: ContestSlug
  problemId: ProblemId | null
}

/** 评估当前会话对比赛/比赛题目的访问；比赛不存在时返回空值。 */
export class EvaluateContestAccess implements APIWithSessionMessage<EvaluateContestAccessResult | null> {
  declare readonly responseType?: EvaluateContestAccessResult | null
  readonly method = 'POST'
  readonly apiPath = 'internal/contests/evaluate-access'
  readonly decodeResponse = decodeEvaluateContestAccessResponse
  private readonly contestSlug: ContestSlug
  private readonly problemId: ProblemId | null

  constructor(contestSlug: ContestSlug, problemId: ProblemId | null) {
    this.contestSlug = contestSlug
    this.problemId = problemId
  }

  body(): EvaluateContestAccessBody {
    return {
      contestSlug: this.contestSlug,
      problemId: this.problemId,
    }
  }
}

function decodeEvaluateContestAccessResponse(value: unknown): EvaluateContestAccessResult | null {
  if (value === null) {
    return null
  }
  if (!isRecord(value)) {
    throw new Error('Invalid contest access response payload.')
  }

  const contestSlug = parseStringBrand(value.contestSlug, parseContestSlug)
  const contestTitle = parseStringBrand(value.contestTitle, parseContestTitle)

  if (typeof value.contestId !== 'string') {
    throw new Error('Invalid contest id.')
  }

  return {
    contestId: value.contestId as ContestId,
    contestSlug,
    contestTitle,
    contestStarted: booleanField(value.contestStarted, 'contestStarted'),
    contestEnded: booleanField(value.contestEnded, 'contestEnded'),
    isRegistered: booleanField(value.isRegistered, 'isRegistered'),
    containsProblem: booleanField(value.containsProblem, 'containsProblem'),
    canViewContest: booleanField(value.canViewContest, 'canViewContest'),
    canViewContestDetail: booleanField(value.canViewContestDetail, 'canViewContestDetail'),
    canManageContest: booleanField(value.canManageContest, 'canManageContest'),
    canViewLinkedContestProblem: booleanField(value.canViewLinkedContestProblem, 'canViewLinkedContestProblem'),
    canManageLinkedContestProblem: booleanField(value.canManageLinkedContestProblem, 'canManageLinkedContestProblem'),
    canSubmitContestProblem: booleanField(value.canSubmitContestProblem, 'canSubmitContestProblem'),
  }
}

function booleanField(value: unknown, fieldName: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(`Invalid contest access ${fieldName}.`)
  }
  return value
}

function parseStringBrand<T>(value: unknown, parse: (raw: string) => { ok: true; value: T } | { ok: false; error: string }): T {
  if (typeof value !== 'string') {
    throw new Error('Expected string field.')
  }
  const parsed = parse(value)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return parsed.value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
