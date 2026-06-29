import { formatDateTime, formatUtcOffsetTitle } from '@/system/format/date-time'

/**
 * 博客时间展示格式化函数，复用系统本地时间格式。
 */
export const formatBlogDate = formatDateTime

/**
 * 博客时间标题格式化函数，显示当前环境的 UTC 偏移。
 */
export const formatBlogDateTitle = formatUtcOffsetTitle

/**
 * 根据博客得分正负返回颜色类名，供列表和详情投票数展示使用。
 */
export function blogScoreClassName(score: number): string {
  if (score > 0) {
    return 'text-emerald-700'
  }

  if (score < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}
