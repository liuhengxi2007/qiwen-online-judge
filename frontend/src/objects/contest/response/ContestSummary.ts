import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { AuditFields } from '@/objects/shared/AuditFields'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ContestRegistrationStatus } from '@/objects/contest/response/ContestRegistrationStatus'

export type ContestSummary = AuditFields & {
  id: ContestId
  slug: ContestSlug
  title: ContestTitle
  description: ContestDescription
  startAt: string
  endAt: string
  accessPolicy: ResourceAccessPolicy
  registrationStatus: ContestRegistrationStatus
  author: UserIdentity | null
}
