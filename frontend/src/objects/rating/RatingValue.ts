export type RatingValue = number & { readonly __brand: 'RatingValue' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createRatingValue(value: number): RatingValue {
  return value as RatingValue
}

export function ratingValue(rating: RatingValue): number {
  return rating
}

export function parseRatingValue(rawRating: number): ParseResult<RatingValue> {
  if (!Number.isFinite(rawRating)) {
    return { ok: false, error: 'Rating value must be a finite number.' }
  }

  return { ok: true, value: createRatingValue(rawRating) }
}

export function formatRatingValue(rating: RatingValue): string {
  return ratingValue(rating).toLocaleString(undefined, {
    maximumFractionDigits: 2,
  })
}
