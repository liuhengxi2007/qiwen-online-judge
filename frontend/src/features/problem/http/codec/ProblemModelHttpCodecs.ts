import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { ProblemData } from '@/features/problem/model/ProblemData'
import type { ProblemId } from '@/features/problem/model/ProblemId'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemSpaceLimitMb } from '@/features/problem/model/ProblemSpaceLimitMb'
import type { ProblemStatementText } from '@/features/problem/model/ProblemStatementText'
import type { ProblemTimeLimitMs } from '@/features/problem/model/ProblemTimeLimitMs'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import {
  parseProblemDataFilename,
  parseProblemId,
  parseProblemSlug,
  parseProblemSpaceLimitMb,
  parseProblemStatementText,
  parseProblemTimeLimitMs,
  parseProblemTitle,
  problemSlugValue,
  problemSpaceLimitMbValue,
  problemStatementTextValue,
  problemTimeLimitMsValue,
  problemTitleValue,
  requireParsed,
} from '@/features/problem/lib/problem-parsers'

export type ProblemIdContract = string
export type ProblemSlugContract = string
export type ProblemTitleContract = string
export type ProblemStatementTextContract = string
export type ProblemDataContract = string | null
export type ProblemTimeLimitMsContract = number
export type ProblemSpaceLimitMbContract = number
export type ProblemTitleDisplayModeContract = 'title' | 'slug' | 'title_with_slug'
export type OthersSubmissionAccessContract = 'none' | 'summary' | 'detail'

export function fromProblemIdContract(value: ProblemIdContract, label: string): ProblemId {
  return requireParsed(parseProblemId(value), label)
}

export function fromProblemSlugContract(value: ProblemSlugContract, label: string): ProblemSlug {
  return requireParsed(parseProblemSlug(value), label)
}

export function toProblemSlugContract(value: ProblemSlug): ProblemSlugContract {
  return problemSlugValue(value)
}

export function fromProblemTitleContract(value: ProblemTitleContract, label: string): ProblemTitle {
  return requireParsed(parseProblemTitle(value), label)
}

export function toProblemTitleContract(value: ProblemTitle): ProblemTitleContract {
  return problemTitleValue(value)
}

export function fromProblemStatementTextContract(
  value: ProblemStatementTextContract,
  label: string,
): ProblemStatementText {
  return requireParsed(parseProblemStatementText(value), label)
}

export function toProblemStatementTextContract(value: ProblemStatementText): ProblemStatementTextContract {
  return problemStatementTextValue(value)
}

export function fromProblemDataContract(value: ProblemDataContract, label: string): ProblemData {
  return {
    value: value === null ? null : requireParsed(parseProblemDataFilename(value), label),
  }
}

export function fromProblemTimeLimitMsContract(value: ProblemTimeLimitMsContract, label: string): ProblemTimeLimitMs {
  return requireParsed(parseProblemTimeLimitMs(value), label)
}

export function toProblemTimeLimitMsContract(value: ProblemTimeLimitMs): ProblemTimeLimitMsContract {
  return problemTimeLimitMsValue(value)
}

export function fromProblemSpaceLimitMbContract(
  value: ProblemSpaceLimitMbContract,
  label: string,
): ProblemSpaceLimitMb {
  return requireParsed(parseProblemSpaceLimitMb(value), label)
}

export function toProblemSpaceLimitMbContract(value: ProblemSpaceLimitMb): ProblemSpaceLimitMbContract {
  return problemSpaceLimitMbValue(value)
}

export function fromProblemTitleDisplayModeContract(
  value: string,
  label: string,
): ProblemTitleDisplayMode {
  switch (value) {
    case 'title':
    case 'slug':
    case 'title_with_slug':
      return value
    default:
      throw new Error(
        `Invalid ${label} in contract payload: Problem title display mode must be one of: title, slug, title_with_slug.`,
      )
  }
}

export function toProblemTitleDisplayModeContract(
  value: ProblemTitleDisplayMode,
): ProblemTitleDisplayModeContract {
  return value
}

export function fromOthersSubmissionAccessContract(
  value: OthersSubmissionAccessContract,
): OthersSubmissionAccess {
  return value
}

export function toOthersSubmissionAccessContract(
  value: OthersSubmissionAccess,
): OthersSubmissionAccessContract {
  return value
}
