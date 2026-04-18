import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'
import { problemSlugValue, problemTitleValue } from '@/features/problem/domain/problem-parsers'

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

export function useProblemTitleDisplayMode(): ProblemTitleDisplayMode {
  return useAuthStore((state) => state.session?.preferences.problemTitleDisplayMode ?? 'title')
}

export function useProblemTitleDisplay(title: ProblemTitle, slug: ProblemSlug): string {
  const mode = useProblemTitleDisplayMode()
  return formatProblemTitleDisplay(title, slug, mode)
}
