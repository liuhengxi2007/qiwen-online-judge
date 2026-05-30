import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

export function useProblemTitleDisplayMode(): ProblemTitleDisplayMode {
  return useAuthStore((state) => state.session?.preferences.problemTitleDisplayMode ?? 'title')
}

export function useProblemTitleDisplay(title: ProblemTitle, slug: ProblemSlug): string {
  const mode = useProblemTitleDisplayMode()
  return formatProblemTitleDisplay(title, slug, mode)
}
