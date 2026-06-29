/** Rating 值品牌类型；由后端计算或管理接口返回。 */
export type RatingValue = number & { readonly __brand: 'RatingValue' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建 rating 品牌值；调用前必须确认数值有限。 */
function createRatingValue(value: number): RatingValue {
  /** 注意：这里的 as 只在 parseRatingValue 校验通过后施加品牌类型。 */
  return value as RatingValue
}

/** 将 rating 品牌值还原为数字；无副作用。 */
export function ratingValue(rating: RatingValue): number {
  return rating
}

/** 解析 rating 数值；拒绝 NaN/Infinity。 */
export function parseRatingValue(rawRating: number): ParseResult<RatingValue> {
  if (!Number.isFinite(rawRating)) {
    return { ok: false, error: 'Rating value must be a finite number.' }
  }

  return { ok: true, value: createRatingValue(rawRating) }
}

/** 格式化 rating 为展示文本；不使用分组分隔符，最多两位小数。 */
export function formatRatingValue(rating: RatingValue): string {
  return ratingValue(rating).toLocaleString(undefined, {
    maximumFractionDigits: 2,
    useGrouping: false,
  })
}
