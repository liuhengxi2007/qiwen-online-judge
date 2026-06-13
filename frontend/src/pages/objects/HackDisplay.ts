import type { HackStatus } from '@/objects/hack/HackStatus'

/**
 * Hack 展示层使用的翻译函数形态，缺失时回退到英文硬编码标签。
 */
type Translate = (key: string, values?: Record<string, string | number>) => string

/**
 * 将 Hack 状态转换为用户可读标签，优先使用当前语言消息表。
 */
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

/**
 * 将 Hack 模式转换为用户可读标签；未知模式原样返回以保留后端信息。
 */
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
