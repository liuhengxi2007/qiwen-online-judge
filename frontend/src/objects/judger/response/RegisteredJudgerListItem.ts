import { readArray, readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type RegisteredJudgerListItem = {
  judgerId: string
  requestedPrefix: string
  host: string
  processId: string | null
  supportedLanguages: string[]
  registeredAt: string
  lastHeartbeatAt: string
}

export function fromRegisteredJudgerListItemContract(value: unknown, label: string): RegisteredJudgerListItem {
  const judger = readRecord(value, label)
  return {
    judgerId: readString(judger.judgerId, `${label} judger id`),
    requestedPrefix: readString(judger.requestedPrefix, `${label} requested prefix`),
    host: readString(judger.host, `${label} host`),
    processId: readNullable(judger.processId, `${label} process id`, readString),
    supportedLanguages: readArray(judger.supportedLanguages, `${label} supported languages`, readString),
    registeredAt: readString(judger.registeredAt, `${label} registered at`),
    lastHeartbeatAt: readString(judger.lastHeartbeatAt, `${label} last heartbeat at`),
  }
}
