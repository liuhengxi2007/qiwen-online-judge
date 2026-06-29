/** 比赛排名品牌类型；表示后端计算后的名次。 */
export type ContestRank = number & { readonly __brand: 'ContestRank' }

/** 将比赛排名品牌值还原为数字；无副作用。 */
export function contestRankValue(rank: ContestRank): number {
  return rank
}
