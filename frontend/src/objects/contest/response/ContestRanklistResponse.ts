import type { ContestRanklistItem } from '@/objects/contest/response/ContestRanklistItem'
import type { PageResponse } from '@/objects/shared/PageResponse'

/** 比赛排行榜分页响应；条目按后端排名规则排序。 */
export type ContestRanklistResponse = PageResponse<ContestRanklistItem>
