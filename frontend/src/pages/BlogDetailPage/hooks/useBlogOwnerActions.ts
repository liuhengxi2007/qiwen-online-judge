import { useState } from 'react'

import { DeleteBlog } from '@/apis/blog/DeleteBlog'
import { UpdateBlog } from '@/apis/blog/UpdateBlog'
import { VoteBlog } from '@/apis/blog/VoteBlog'
import { parseBlogContent } from '@/objects/blog/BlogContent'
import { parseBlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import type { BlogVote } from '@/objects/blog/BlogVote'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

type UseBlogOwnerActionsArgs = {
  blog: BlogDetail | null
  onDeleted: () => void
  setBlog: (blog: BlogDetail | null) => void
  setCommentErrorMessage: (message: string) => void
}

export function useBlogOwnerActions({ blog, onDeleted, setBlog, setCommentErrorMessage }: UseBlogOwnerActionsArgs) {
  const { t } = useI18n()
  const [isVoting, setIsVoting] = useState(false)
  const [isEditingBlog, setIsEditingBlog] = useState(false)
  const [editBlogTitle, setEditBlogTitle] = useState('')
  const [editBlogContent, setEditBlogContent] = useState('')
  const [editBlogVisibility, setEditBlogVisibility] = useState<BlogVisibility>('public')

  async function submitVote(vote: BlogVote) {
    if (!blog) {
      return
    }

    setIsVoting(true)
    try {
      setBlog(await sendAPI(new VoteBlog(blog.id, { vote })))
    } finally {
      setIsVoting(false)
    }
  }

  function startEditingBlog() {
    if (!blog) {
      return
    }

    setEditBlogTitle(blog.title)
    setEditBlogContent(blog.content)
    setEditBlogVisibility(blog.visibility)
    setCommentErrorMessage('')
    setIsEditingBlog(true)
  }

  async function submitBlogEdit() {
    if (!blog) {
      return
    }

    const parsedTitle = parseBlogTitle(editBlogTitle)
    const parsedContent = parseBlogContent(editBlogContent)
    if (!parsedTitle.ok) {
      setCommentErrorMessage(parsedTitle.error)
      return
    }
    if (!parsedContent.ok) {
      setCommentErrorMessage(parsedContent.error)
      return
    }

    setBlog(
      await sendAPI(
        new UpdateBlog(blog.id, {
          title: parsedTitle.value,
          content: parsedContent.value,
          visibility: editBlogVisibility,
        }),
      ),
    )
    setIsEditingBlog(false)
  }

  async function removeBlog() {
    if (!blog || !window.confirm(t('blog.delete.confirm'))) {
      return
    }

    await sendAPI(new DeleteBlog(blog.id))
    onDeleted()
  }

  return {
    isVoting,
    isEditingBlog,
    setIsEditingBlog,
    editBlogTitle,
    setEditBlogTitle,
    editBlogContent,
    setEditBlogContent,
    editBlogVisibility,
    setEditBlogVisibility,
    submitVote,
    startEditingBlog,
    submitBlogEdit,
    removeBlog,
  }
}
