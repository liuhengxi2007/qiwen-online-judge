import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import { problemSlugValue, problemTitleValue } from '@/objects/problem/problem-parsers'

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
