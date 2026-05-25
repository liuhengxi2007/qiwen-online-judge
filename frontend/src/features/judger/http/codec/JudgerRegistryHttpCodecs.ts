import type { RegisteredJudgerListItem } from '@/features/judger/model/response/RegisteredJudgerListItem'

type RegisteredJudgerListItemContract = {
  judgerId: string
  requestedPrefix: string
  host: string
  processId: string | null
  supportedLanguages: string[]
  registeredAt: string
  lastHeartbeatAt: string
}

export function fromRegisteredJudgerListItemContract(response: RegisteredJudgerListItemContract): RegisteredJudgerListItem {
  return {
    judgerId: response.judgerId,
    requestedPrefix: response.requestedPrefix,
    host: response.host.trim(),
    processId: response.processId?.trim() || null,
    supportedLanguages: response.supportedLanguages.map((language) => language.trim()).filter((language) => language.length > 0),
    registeredAt: response.registeredAt,
    lastHeartbeatAt: response.lastHeartbeatAt,
  }
}
