import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { formatProblemTitleDisplay } from '@/pages/objects/ProblemTitleDisplay'
import type { ProblemTitleDisplayMode } from '@/objects/problem/ProblemTitleDisplayMode'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'

/**
 * 从当前会话偏好读取题目标题展示模式，未登录或缺失时默认显示标题。
 */
export function useProblemTitleDisplayMode(): ProblemTitleDisplayMode {
  return useAuthStore((state) => state.session?.preferences.problemTitleDisplayMode ?? 'title')
}

/**
 * 按当前用户偏好格式化题目标题展示文本。
 */
export function useProblemTitleDisplay(title: ProblemTitle, slug: ProblemSlug): string {
  const mode = useProblemTitleDisplayMode()
  return formatProblemTitleDisplay(title, slug, mode)
}
