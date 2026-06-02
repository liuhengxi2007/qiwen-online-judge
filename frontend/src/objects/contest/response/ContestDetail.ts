import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestProblemSummary } from '@/objects/contest/ContestProblemSummary'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

export type ContestDetail = {
  id: ContestId
  slug: ContestSlug
  title: ContestTitle
  description: ContestDescription
  startAt: string
  endAt: string
  problems: ContestProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  author: UserIdentity | null
  createdAt: string
  updatedAt: string
}
