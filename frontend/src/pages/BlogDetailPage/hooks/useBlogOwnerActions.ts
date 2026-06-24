import { useState } from 'react'

import { DeleteBlog } from '@/apis/blog/DeleteBlog'
import { UpdateBlog } from '@/apis/blog/UpdateBlog'
import { VoteBlog } from '@/apis/blog/VoteBlog'
import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { BlogVote } from '@/objects/blog/BlogVote'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { createRestrictedVisibilityPolicy } from '@/objects/shared/access/ResourceVisibilityPolicy'
import {
  buildResourceVisibilityPolicy,
  grantedGroupsInputFromVisibilityPolicy,
  grantedUsersInputFromVisibilityPolicy,
} from '@/pages/components/ResourceAccessEditorInput'
import { validateBlogFormDraft } from '@/pages/objects/BlogForm'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 博客作者操作 hook 参数，提供当前博客、详情替换、删除完成回调和共享错误消息入口。
 */
type UseBlogOwnerActionsArgs = {
  blog: BlogDetail | null
  onDeleted: () => void
  setBlog: (blog: BlogDetail | null) => void
  setCommentErrorMessage: (message: string) => void
}

/**
 * 博客作者操作 hook，维护投票、编辑表单和删除博客副作用。
 * 更新和投票成功后会用后端返回的博客详情替换页面状态，删除成功后交给页面跳转。
 */
export function useBlogOwnerActions({ blog, onDeleted, setBlog, setCommentErrorMessage }: UseBlogOwnerActionsArgs) {
  const { t } = useI18n()
  const [isVoting, setIsVoting] = useState(false)
  const [isEditingBlog, setIsEditingBlog] = useState(false)
  const [editBlogTitle, setEditBlogTitle] = useState('')
  const [editBlogContent, setEditBlogContent] = useState('')
  const [editBlogBaseAccess, setEditBlogBaseAccess] = useState<BaseAccess>('public')
  const [editBlogGrantedUsersInput, setEditBlogGrantedUsersInput] = useState('')
  const [editBlogGrantedGroupsInput, setEditBlogGrantedGroupsInput] = useState('')
  const editBlogAccessPolicyResult = buildResourceVisibilityPolicy(
    editBlogBaseAccess,
    editBlogGrantedUsersInput,
    editBlogGrantedGroupsInput,
  )
  const editBlogAccessPolicy = editBlogAccessPolicyResult.ok
    ? editBlogAccessPolicyResult.value
    : (blog?.visibilityPolicy ?? createRestrictedVisibilityPolicy())

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
    setEditBlogBaseAccess(blog.visibilityPolicy.baseAccess)
    setEditBlogGrantedUsersInput(grantedUsersInputFromVisibilityPolicy(blog.visibilityPolicy))
    setEditBlogGrantedGroupsInput(grantedGroupsInputFromVisibilityPolicy(blog.visibilityPolicy))
    setCommentErrorMessage('')
    setIsEditingBlog(true)
  }

  async function submitBlogEdit() {
    if (!blog) {
      return
    }

    const validation = validateBlogFormDraft({
      title: editBlogTitle,
      content: editBlogContent,
      baseAccess: editBlogBaseAccess,
      grantedUsersInput: editBlogGrantedUsersInput,
      grantedGroupsInput: editBlogGrantedGroupsInput,
    })
    if (!validation.ok) {
      setCommentErrorMessage(validation.message)
      return
    }

    setBlog(
      await sendAPI(
        new UpdateBlog(blog.id, validation.request),
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
    editBlogAccessPolicy,
    editBlogBaseAccess,
    setEditBlogBaseAccess,
    editBlogGrantedUsersInput,
    setEditBlogGrantedUsersInput,
    editBlogGrantedGroupsInput,
    setEditBlogGrantedGroupsInput,
    submitVote,
    startEditingBlog,
    submitBlogEdit,
    removeBlog,
  }
}
