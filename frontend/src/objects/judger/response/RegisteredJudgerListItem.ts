/** 已注册 judger 列表项；用于管理视图查看 worker 能力和心跳状态。 */
export type RegisteredJudgerListItem = {
  judgerId: string
  requestedPrefix: string
  host: string
  processId: string | null
  supportedLanguages: string[]
  registeredAt: string
  lastHeartbeatAt: string
}
