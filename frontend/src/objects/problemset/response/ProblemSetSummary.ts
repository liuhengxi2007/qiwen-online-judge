import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { ProblemSetDescription } from '@/objects/problemset/ProblemSetDescription'
import { fromProblemSetDescriptionContract } from '@/objects/problemset/ProblemSetDescription'
import type { ProblemSetId } from '@/objects/problemset/ProblemSetId'
import { fromProblemSetIdContract } from '@/objects/problemset/ProblemSetId'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { fromProblemSetSlugContract } from '@/objects/problemset/ProblemSetSlug'
import type { ProblemSetTitle } from '@/objects/problemset/ProblemSetTitle'
import { fromProblemSetTitleContract } from '@/objects/problemset/ProblemSetTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { fromResourceAccessPolicyContract } from '@/objects/shared/access/ResourceAccessPolicy'
import type { AuditFields } from '@/objects/shared/AuditFields'
import { fromAuditFieldsContract } from '@/objects/shared/AuditFields'
import { readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ProblemSetSummary = AuditFields & {
  id: ProblemSetId
  slug: ProblemSetSlug
  title: ProblemSetTitle
  description: ProblemSetDescription
  accessPolicy: ResourceAccessPolicy
  author: UserIdentity | null
}

export function fromProblemSetSummaryContract(value: unknown, label: string): ProblemSetSummary {
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
    accessPolicy: fromResourceAccessPolicyContract(problemSet.accessPolicy),
    author: readNullable(problemSet.author, `${label} author`, fromUserIdentityContract),
  }
}
