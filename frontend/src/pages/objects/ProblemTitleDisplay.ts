import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { problemTitleValue } from '@/objects/problem/ProblemTitle'

/**
 * 根据用户偏好格式化题目标题展示，支持只显示 slug、只显示标题或标题附带 slug。
 */
export function formatProblemTitleDisplay(
  title: ProblemTitle,
  slug: ProblemSlug,
  mode: ProblemTitleDisplayMode,
): string {
  const titleText = problemTitleValue(title)
  const slugText = problemSlugValue(slug)

  switch (mode) {
    case 'slug':
      return slugText
    case 'title_with_slug':
      return `${titleText} (${slugText})`
    case 'title':
    default:
      return titleText
  }
}

/**
 * 判断当前展示模式下是否需要额外补充题目 slug，避免标题模式中丢失可定位标识。
 */
export function shouldShowProblemSlugSupplement(mode: ProblemTitleDisplayMode): boolean {
  return mode === 'title'
}
