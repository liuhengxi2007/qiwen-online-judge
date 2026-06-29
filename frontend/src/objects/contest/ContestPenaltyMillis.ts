/** 比赛罚时毫秒品牌类型；用于排名和题目结果展示。 */
export type ContestPenaltyMillis = number & { readonly __brand: 'ContestPenaltyMillis' }

/** 将比赛罚时品牌值还原为毫秒数；无副作用。 */
export function contestPenaltyMillisValue(penaltyMillis: ContestPenaltyMillis): number {
  return penaltyMillis
}
