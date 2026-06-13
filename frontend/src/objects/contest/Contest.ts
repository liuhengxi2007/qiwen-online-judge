import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestProblemSummary } from '@/objects/contest/ContestProblemSummary'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

/** 比赛完整对象；包含时间窗口、题目列表、访问策略、作者和审计时间。 */
export type Contest = {
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
