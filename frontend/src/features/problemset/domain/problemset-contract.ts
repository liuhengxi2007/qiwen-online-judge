import type {
  CreateProblemSetRequest as CreateProblemSetRequestContract,
  LinkProblemRequest as LinkProblemRequestContract,
  ProblemSetDetail as ProblemSetDetailContract,
  ProblemSetListResponse as ProblemSetListResponseContract,
  ProblemSetProblemSummary as ProblemSetProblemSummaryContract,
  ProblemSetSummary as ProblemSetSummaryContract,
  UpdateProblemSetRequest as UpdateProblemSetRequestContract,
} from '@contracts/problemset'
import { fromUserIdentityContract } from '@/features/auth/domain/auth'
import {
  parseProblemId,
  parseProblemSlug,
  parseProblemTitle,
  problemSlugValue,
} from '@/features/problem/domain/problem'
import type { AddProblemToProblemSetRequest } from '@/features/problemset/model/AddProblemToProblemSetRequest'
import type { CreateProblemSetRequest } from '@/features/problemset/model/CreateProblemSetRequest'
import type { ProblemSetDetail } from '@/features/problemset/model/ProblemSetDetail'
import type { ProblemSetListResponse } from '@/features/problemset/model/ProblemSetListResponse'
import type { ProblemSetProblemSummary } from '@/features/problemset/model/ProblemSetProblemSummary'
import type { ProblemSetSummary } from '@/features/problemset/model/ProblemSetSummary'
import type { UpdateProblemSetRequest } from '@/features/problemset/model/UpdateProblemSetRequest'
import {
  parseProblemSetDescription,
  parseProblemSetId,
  parseProblemSetProblemPosition,
  parseProblemSetSlug,
  parseProblemSetTitle,
  problemSetDescriptionValue,
  problemSetSlugValue,
  problemSetTitleValue,
  requireParsed,
} from '@/features/problemset/domain/problemset-parsers'

export function fromProblemSetProblemSummaryContract(
  problem: ProblemSetProblemSummaryContract,
): ProblemSetProblemSummary {
  return {
    id: requireParsed(parseProblemId(problem.id), 'problem set problem id'),
    slug: requireParsed(parseProblemSlug(problem.slug), 'problem set problem slug'),
    title: requireParsed(parseProblemTitle(problem.title), 'problem set problem title'),
    position: requireParsed(parseProblemSetProblemPosition(problem.position), 'problem set problem position'),
  }
}

export function fromProblemSetSummaryContract(problemSet: ProblemSetSummaryContract): ProblemSetSummary {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set summary id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set summary slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set summary title'),
    description: requireParsed(parseProblemSetDescription(problemSet.description), 'problem set summary description'),
    accessPolicy: problemSet.accessPolicy,
    creator: fromUserIdentityContract(problemSet.creator),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetDetailContract(problemSet: ProblemSetDetailContract): ProblemSetDetail {
  return {
    id: requireParsed(parseProblemSetId(problemSet.id), 'problem set detail id'),
    slug: requireParsed(parseProblemSetSlug(problemSet.slug), 'problem set detail slug'),
    title: requireParsed(parseProblemSetTitle(problemSet.title), 'problem set detail title'),
    description: requireParsed(parseProblemSetDescription(problemSet.description), 'problem set detail description'),
    problems: problemSet.problems.map(fromProblemSetProblemSummaryContract),
    accessPolicy: problemSet.accessPolicy,
    creator: fromUserIdentityContract(problemSet.creator),
    createdAt: problemSet.createdAt,
    updatedAt: problemSet.updatedAt,
  }
}

export function fromProblemSetListResponseContract(
  response: ProblemSetListResponseContract,
): ProblemSetListResponse {
  return {
    items: response.items.map(fromProblemSetSummaryContract),
    page: response.page,
    pageSize: response.pageSize,
    totalItems: response.totalItems,
  }
}

export function toCreateProblemSetRequestContract(
  request: CreateProblemSetRequest,
): CreateProblemSetRequestContract {
  return {
    slug: problemSetSlugValue(request.slug),
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: request.accessPolicy,
  }
}

export function toUpdateProblemSetRequestContract(
  request: UpdateProblemSetRequest,
): UpdateProblemSetRequestContract {
  return {
    title: problemSetTitleValue(request.title),
    description: problemSetDescriptionValue(request.description),
    accessPolicy: request.accessPolicy,
  }
}

export function toLinkProblemRequestContract(
  request: AddProblemToProblemSetRequest,
): LinkProblemRequestContract {
  return {
    problemSlug: problemSlugValue(request.problemSlug),
  }
}
