import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import { problemSlugValue, problemTitleValue } from '@/features/problem/lib/problem-parsers'

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

export function shouldShowProblemSlugSupplement(mode: ProblemTitleDisplayMode): boolean {
  return mode === 'title'
}
