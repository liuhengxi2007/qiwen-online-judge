import type { RegisteredJudgerListItem as RegisteredJudgerListItemContract } from '@contracts/judger'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'

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
