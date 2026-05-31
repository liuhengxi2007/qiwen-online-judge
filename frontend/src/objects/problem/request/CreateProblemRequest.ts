import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemStatementText } from '@/objects/problem/ProblemStatementText'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

export type CreateProblemRequest = {
  slug: ProblemSlug
  title: ProblemTitle
  statement: ProblemStatementText
  accessPolicy: ResourceAccessPolicy
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}
