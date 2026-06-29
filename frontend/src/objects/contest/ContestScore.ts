/** 比赛得分品牌类型；内部按 0 到 1 的比例值传输。 */
export type ContestScore = number & { readonly __brand: 'ContestScore' }

/** 将比赛得分品牌值还原为比例数字；无副作用。 */
export function contestScoreValue(score: ContestScore): number {
  return score
}

/** 格式化比赛得分为百分制展示文本；仅用于前端显示。 */
export function formatContestScore(score: ContestScore): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 2,
  }).format(contestScoreValue(score) * 100)
}
