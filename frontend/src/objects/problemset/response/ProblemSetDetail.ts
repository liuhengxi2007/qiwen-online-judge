import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import { fromProblemSetDescriptionContract } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import { fromProblemSetIdContract } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetProblemSummary } from '@/objects/problemset/ProblemSetProblemSummary'
import { fromProblemSetProblemSummaryContract } from '@/objects/problemset/ProblemSetProblemSummary'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { fromProblemSetSlugContract } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import { fromProblemSetTitleContract } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { fromResourceAccessPolicyContract } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'
import { fromAuditFieldsContract } from '@/objects/shared/AuditFields'
import { readArray, readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemSetDetail = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  problems: ProblemSetProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  author: UserIdentity | null
}

export function fromProblemSetDetailContract(value: unknown, label = 'problem set detail'): ProblemSetDetail {
  const problemSet = readRecord(value, label)
  return {
    ...fromAuditFieldsContract(value, label),
    id: fromProblemSetIdContract(readString(problemSet.id, `${label} id`), `${label} id`),
    slug: fromProblemSetSlugContract(readString(problemSet.slug, `${label} slug`), `${label} slug`),
    title: fromProblemSetTitleContract(readString(problemSet.title, `${label} title`), `${label} title`),
    description: fromProblemSetDescriptionContract(
      readString(problemSet.description, `${label} description`),
      `${label} description`,
    ),
    problems: readArray(problemSet.problems, `${label} problems`, fromProblemSetProblemSummaryContract),
    accessPolicy: fromResourceAccessPolicyContract(problemSet.accessPolicy),
    author: readNullable(problemSet.author, `${label} author`, fromUserIdentityContract),
  }
}
