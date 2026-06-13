import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { Username } from '@/objects/user/Username'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 比赛 rating 快照中的参赛者数据；用于内部 rating 计算管理视图。 */
type ContestRatingSnapshotParticipant = {
  username: Username
  rank: number
  totalScore: number
  penaltyMillis: number
}

/** 比赛 rating 快照；包含比赛时间和参与者排名/分数/罚时。 */
type ContestRatingSnapshot = {
  slug: ContestSlug
  title: ContestTitle
  startAt: string
  endAt: string
  participants: ContestRatingSnapshotParticipant[]
}

/** 获取比赛 rating 快照；输入比赛 slug，输出内部 rating 计算所需数据。 */
export class GetContestRatingSnapshot implements APIWithSessionMessage<ContestRatingSnapshot> {
  declare readonly responseType?: ContestRatingSnapshot
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `internal/contests/${contestSlugValue(contestSlug)}/rating-snapshot`
  }

  body(): undefined {
    return undefined
  }
}
