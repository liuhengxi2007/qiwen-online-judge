import { SubmissionListPageContent } from '@/pages/components/SubmissionListPageContent'

/**
 * 全站提交列表页，直接复用通用提交列表内容组件，不预设题目过滤条件。
 */
export function SubmissionPage() {
  // 保留共享提交列表内容组件：这里和 ProblemSubmissionPage、ContestSubmissionPage 共用筛选、分页和列表逻辑。
  return <SubmissionListPageContent />
}
