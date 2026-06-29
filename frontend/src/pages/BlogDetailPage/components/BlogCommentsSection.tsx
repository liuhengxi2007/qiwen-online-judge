import { BlogCommentThread } from './BlogCommentThread'
import type { useBlogDetailPageModel } from '../hooks/useBlogDetailPageModel'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import { useI18n } from '@/system/i18n/use-i18n'

import { BlogCommentComposer } from './BlogCommentComposer'

/**
 * 博客详情页模型类型别名，供评论区读取评论草稿和操作回调。
 */
type BlogDetailPageModel = ReturnType<typeof useBlogDetailPageModel>

/**
 * 评论区属性，包含博客详情、当前用户名和页面模型。
 */
type BlogCommentsSectionProps = {
  blog: BlogDetail
  currentUsername: string
  model: BlogDetailPageModel
}

/**
 * 博客评论区，组合发表评论输入和评论树，并把所有操作回调透传给评论树组件。
 */
export function BlogCommentsSection({ blog, currentUsername, model }: BlogCommentsSectionProps) {
  const { t } = useI18n()

  return (
    <div className="mt-8 space-y-5">
      <div>
        <h2 className="text-lg font-semibold text-slate-950">{t('blog.comment.heading')}</h2>
        <p className="mt-1 text-sm text-slate-500">{t('blog.comment.description')}</p>
      </div>
      <BlogCommentComposer model={model} />
      <div className="space-y-3">
        <h3 className="text-base font-semibold text-slate-950">{t('blog.comment.heading')}</h3>
        <BlogCommentThread
          comments={blog.comments}
          currentUsername={currentUsername}
          votingCommentId={model.votingCommentId}
          replyTargetId={model.replyTargetId}
          replyContent={model.replyContent}
          isSubmittingReply={model.isSubmittingReply}
          editingCommentId={model.editingCommentId}
          editingCommentContent={model.editingCommentContent}
          commentErrorMessage={model.commentErrorMessage}
          onReplyTargetChange={model.setReplyTargetId}
          onReplyContentChange={model.setReplyContent}
          onEditingCommentIdChange={model.setEditingCommentId}
          onEditingCommentContentChange={model.setEditingCommentContent}
          onSubmitReply={(commentId) => void model.submitReply(commentId)}
          onSubmitCommentVote={(commentId, vote) => void model.submitCommentVote(commentId, vote)}
          onStartEditingComment={model.startEditingComment}
          onSubmitCommentEdit={(commentId) => void model.submitCommentEdit(commentId)}
          onRemoveComment={(commentId) => void model.removeComment(commentId)}
        />
      </div>
    </div>
  )
}
