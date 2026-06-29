import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { useBlogCommentActions } from './useBlogCommentActions'
import { useBlogOwnerActions } from './useBlogOwnerActions'
import { useBlogProblemSubmissionActions } from './useBlogProblemSubmissionActions'

/**
 * 博客详情页模型参数，包含当前博客快照、替换回调和删除后的跳转回调。
 */
type UseBlogDetailPageModelArgs = {
  blog: BlogDetail | null
  setBlog: (blog: BlogDetail | null) => void
  onDeleted: () => void
}

/**
 * 博客详情页模型 hook，聚合作者操作、评论操作和提交到题目的动作状态。
 */
export function useBlogDetailPageModel({ blog, setBlog, onDeleted }: UseBlogDetailPageModelArgs) {
  const commentActions = useBlogCommentActions({ blog, setBlog })
  const ownerActions = useBlogOwnerActions({
    blog,
    setBlog,
    onDeleted,
    setCommentErrorMessage: commentActions.setCommentErrorMessage,
  })
  const problemSubmissionActions = useBlogProblemSubmissionActions({ blog })

  return {
    ...ownerActions,
    ...commentActions,
    ...problemSubmissionActions,
  }
}
