import { describe, expect, it } from 'vitest'

import { GetContestRatingSnapshot } from '@/apis/contest/GetContestRatingSnapshot'
import { AppendRatingContest } from '@/apis/rating/AppendRatingContest'
import { GetRatingManageState } from '@/apis/rating/GetRatingManageState'
import { ListRatingRanklist } from '@/apis/rating/ListRatingRanklist'
import { PopRatingContest } from '@/apis/rating/PopRatingContest'
import { apiPath, apiRoutePath } from '@/system/api/api-message'
import { parseContestSlug } from '@/objects/contest/ContestSlug'

function contestSlug(raw: string) {
  const parsed = parseContestSlug(raw)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return parsed.value
}

describe('rating API paths', () => {
  it('normalizes raw API route paths', () => {
    expect(apiRoutePath('realtime/events')).toBe('/api/realtime/events')
    expect(apiRoutePath('/api/realtime/events')).toBe('/api/realtime/events')
  })

  it('builds rating ranklist and management paths', () => {
    expect(apiPath(new ListRatingRanklist({ page: 2, pageSize: 10 }))).toBe('/api/ratings/ranklist?page=2&pageSize=10')
    expect(apiPath(new GetRatingManageState())).toBe('/api/ratings/manage')
    expect(apiPath(new AppendRatingContest({ contestSlug: contestSlug('spring-challenge'), m: 60 }))).toBe('/api/ratings/manage/contests')
    expect(apiPath(new PopRatingContest())).toBe('/api/ratings/manage/contests/pop')
  })

  it('builds the contest rating snapshot internal path', () => {
    expect(apiPath(new GetContestRatingSnapshot(contestSlug('spring-challenge')))).toBe(
      '/api/internal/contests/spring-challenge/rating-snapshot',
    )
  })
})
