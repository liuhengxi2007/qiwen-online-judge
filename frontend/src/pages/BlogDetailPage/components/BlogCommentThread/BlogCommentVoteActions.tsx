import { ThumbsDown, ThumbsUp } from 'lucide-react'

import { Button } from '@/components/ui/button'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogVote } from '@/objects/blog/BlogVote'
import { useI18n } from '@/system/i18n/use-i18n'

type BlogCommentVoteActionsProps = {
  commentId: BlogCommentId
  viewerVote: BlogVote | null
  isVoting: boolean
  onVote: (commentId: BlogCommentId, vote: BlogVote) => void
}

export function BlogCommentVoteActions({
  commentId,
  viewerVote,
  isVoting,
  onVote,
}: BlogCommentVoteActionsProps) {
  const { t } = useI18n()

  return (
    <>
      <Button
        type="button"
        size="sm"
        variant={viewerVote === 'up' ? 'default' : 'outline'}
        className={
          viewerVote === 'up'
            ? 'h-8 rounded-xl bg-emerald-600 px-2 text-xs text-white hover:bg-emerald-700'
            : 'h-8 rounded-xl border-emerald-200 bg-white px-2 text-xs text-emerald-700'
        }
        disabled={isVoting}
        onClick={() => onVote(commentId, 'up')}
      >
        <ThumbsUp className="size-3" />
        {t('blog.vote.up')}
      </Button>
      <Button
        type="button"
        size="sm"
        variant={viewerVote === 'down' ? 'default' : 'outline'}
        className={
          viewerVote === 'down'
            ? 'h-8 rounded-xl bg-rose-600 px-2 text-xs text-white hover:bg-rose-700'
            : 'h-8 rounded-xl border-rose-200 bg-white px-2 text-xs text-rose-700'
        }
        disabled={isVoting}
        onClick={() => onVote(commentId, 'down')}
      >
        <ThumbsDown className="size-3" />
        {t('blog.vote.down')}
      </Button>
    </>
  )
}
