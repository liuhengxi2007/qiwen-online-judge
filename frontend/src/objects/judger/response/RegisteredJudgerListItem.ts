export type RegisteredJudgerListItem = {
  judgerId: string
  requestedPrefix: string
  host: string
  processId: string | null
  supportedLanguages: string[]
  registeredAt: string
  lastHeartbeatAt: string
}
