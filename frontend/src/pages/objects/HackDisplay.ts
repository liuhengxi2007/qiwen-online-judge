import type { HackStatus } from '@/objects/hack/HackStatus'

type Translate = (key: string, values?: Record<string, string | number>) => string

export function hackStatusLabel(status: HackStatus, t?: Translate): string {
  const key = `hack.status.${status}`

  if (t) {
    return t(key)
  }

  switch (status) {
    case 'queued':
      return 'Queued'
    case 'running':
      return 'Running'
    case 'success':
      return 'Success'
    case 'no_effect':
      return 'No effect'
    case 'invalid':
      return 'Invalid'
    case 'failed':
      return 'Failed'
  }
}

export function hackModeLabel(mode: string, t?: Translate): string {
  switch (mode) {
    case 'traditional':
      return t ? t('hack.mode.traditional') : 'Traditional'
    case 'interactive':
      return t ? t('hack.mode.interactive') : 'Interactive'
    default:
      return mode
  }
}
