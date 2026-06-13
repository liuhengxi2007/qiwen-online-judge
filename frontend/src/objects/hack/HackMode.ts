/** Hack 目标子任务运行模式；镜像判题任务 mode.type 当前支持的公开取值。 */
export type HackMode = 'traditional' | 'interactive'

const supportedHackModes = ['traditional', 'interactive'] as const satisfies readonly HackMode[]

/** 判断字符串是否为受支持 Hack 模式。 */
export function isHackMode(value: string): value is HackMode {
  /** 注意：includes 需要把待测字符串临时断言为枚举联合类型，不会改变运行时值。 */
  return supportedHackModes.includes(value as HackMode)
}
