import type { ContestDescription } from '@/objects/contest/ContestDescription'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { ResourceAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'

/** 创建比赛请求体；时间为后端约定的字符串格式，权限策略显式传入。 */
export type CreateContestRequest = {
  slug: ContestSlug
  title: ContestTitle
  description: ContestDescription
  startAt: string
  endAt: string
  accessPolicy: ResourceAccessPolicy
}
