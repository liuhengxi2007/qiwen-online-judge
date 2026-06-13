/** 比赛内题目别名品牌类型；通常用于 A/B/C 等比赛题号显示。 */
export type ContestProblemAlias = string & { readonly __brand: 'ContestProblemAlias' }

/** 将比赛题目别名品牌值还原为字符串；无副作用。 */
export function contestProblemAliasValue(alias: ContestProblemAlias): string {
  return alias
}
