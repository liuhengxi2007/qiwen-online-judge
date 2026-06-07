import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'
import type { Username } from '@/objects/user/Username'
import type { APIWithSessionMessage } from '@/system/api/api-message'

type ContestRatingSnapshotParticipant = {
  username: Username
  rank: number
  totalScore: number
  penaltyMillis: number
}

type ContestRatingSnapshot = {
  slug: ContestSlug
  title: ContestTitle
  startAt: string
  endAt: string
  participants: ContestRatingSnapshotParticipant[]
}

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
