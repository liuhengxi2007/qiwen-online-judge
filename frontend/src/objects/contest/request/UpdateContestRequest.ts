import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

/** 更新比赛请求体；允许修改标题、描述、时间窗口和访问策略。 */
export type UpdateContestRequest = {
  title: ContestTitle
  description: ContestDescription
  startAt: string
  endAt: string
  accessPolicy: ResourceAccessPolicy
}
