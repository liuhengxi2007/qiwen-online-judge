import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { formatProblemTitleDisplay } from '@/features/problem/lib/problem-display'
import type { ProblemTitleDisplayMode } from '@/features/problem/model/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import type { ProblemTitle } from '@/features/problem/model/ProblemTitle'

export function useProblemTitleDisplayMode(): ProblemTitleDisplayMode {
  return useAuthStore((state) => state.session?.preferences.problemTitleDisplayMode ?? 'title')
}

export function useProblemTitleDisplay(title: ProblemTitle, slug: ProblemSlug): string {
  const mode = useProblemTitleDisplayMode()
  return formatProblemTitleDisplay(title, slug, mode)
}
