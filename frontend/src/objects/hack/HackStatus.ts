/** Hack 判定状态；覆盖排队、运行、成功、无效、失败等阶段。 */
export type HackStatus = 'queued' | 'running' | 'success' | 'no_effect' | 'invalid' | 'failed'

const supportedHackStatuses = ['queued', 'running', 'success', 'no_effect', 'invalid', 'failed'] as const satisfies readonly HackStatus[]

/** 判断字符串是否为受支持 Hack 状态。 */
export function isHackStatus(value: string): value is HackStatus {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedHackStatuses.includes(value as HackStatus)
}
