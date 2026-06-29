/** 用户贡献值品牌类型；表示后端计算的贡献积分。 */
export type UserContribution = number & { readonly __brand: 'UserContribution' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建贡献值品牌；调用前必须确认数值有限。 */
function createUserContribution(value: number): UserContribution {
  /** 注意：这里的 as 只在 parseUserContribution 校验通过后施加品牌类型。 */
  return value as UserContribution
}

/** 将贡献值品牌还原为数字；无副作用。 */
export function userContributionValue(contribution: UserContribution): number {
  return contribution
}

/** 解析贡献值；只接受有限数字，避免 NaN/Infinity 进入业务展示。 */
export function parseUserContribution(rawContribution: number): ParseResult<UserContribution> {
  if (!Number.isFinite(rawContribution)) {
    return { ok: false, error: 'User contribution must be a finite number.' }
  }

  return { ok: true, value: createUserContribution(rawContribution) }
}
