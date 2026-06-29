import type { ContestRegistrant } from '@/objects/contest/response/ContestRegistrant'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 比赛报名用户分页响应；用于管理或公开报名列表展示。 */
export type ContestRegistrantListResponse = PageResponse<ContestRegistrant>
