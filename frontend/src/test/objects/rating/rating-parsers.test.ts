import { describe, expect, it } from 'vitest'

import { formatRatingValue, parseRatingValue, ratingValue } from '@/objects/rating/RatingValue'

describe('rating-parsers', () => {
  it('validates rating values as finite numbers', () => {
    expect(parseRatingValue(Number.NaN)).toEqual({
      ok: false,
      error: 'Rating value must be a finite number.',
    })
    expect(parseRatingValue(Number.POSITIVE_INFINITY)).toEqual({
      ok: false,
      error: 'Rating value must be a finite number.',
    })

    const parsed = parseRatingValue(1500.25)
    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(ratingValue(parsed.value)).toBe(1500.25)
    }
  })

  it('formats rating values without grouping separators', () => {
    const parsed = parseRatingValue(1500)
    expect(parsed.ok).toBe(true)
    if (parsed.ok) {
      expect(formatRatingValue(parsed.value)).toBe('1500')
    }
  })
})
