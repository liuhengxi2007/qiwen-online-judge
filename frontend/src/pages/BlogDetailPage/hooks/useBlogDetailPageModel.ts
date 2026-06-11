import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { useBlogCommentActions } from './useBlogCommentActions'
import { useBlogOwnerActions } from './useBlogOwnerActions'
import { useBlogProblemSubmissionActions } from './useBlogProblemSubmissionActions'

type UseBlogDetailPageModelArgs = {
  blog: BlogDetail | null
  setBlog: (blog: BlogDetail | null) => void
  onDeleted: () => void
}

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
