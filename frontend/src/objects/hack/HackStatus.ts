export type HackStatus = 'queued' | 'running' | 'success' | 'no_effect' | 'invalid' | 'failed'

const supportedHackStatuses = ['queued', 'running', 'success', 'no_effect', 'invalid', 'failed'] as const satisfies readonly HackStatus[]

export function isHackStatus(value: string): value is HackStatus {
  return supportedHackStatuses.includes(value as HackStatus)
}
