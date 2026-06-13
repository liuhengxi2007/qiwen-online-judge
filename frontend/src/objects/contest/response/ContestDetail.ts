import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestProblemSummary } from '@/objects/contest/ContestProblemSummary'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import type { ContestRegistrationStatus } from '@/objects/contest/response/ContestRegistrationStatus'

/** 比赛详情响应；包含题目列表、报名状态和当前会话管理能力。 */
export type ContestDetail = {
  id: ContestId
  slug: ContestSlug
  title: ContestTitle
  description: ContestDescription
  startAt: string
  endAt: string
  problems: ContestProblemSummary[]
  accessPolicy: ResourceAccessPolicy
  registrationStatus: ContestRegistrationStatus
  canManage: boolean
  author: UserIdentity | null
  createdAt: string
  updatedAt: string
}
