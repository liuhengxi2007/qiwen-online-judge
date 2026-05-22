import type {
  BlogDetail,
  VoteBlogRequest,
} from '@/features/blog/domain/blog'
import { blogIdValue } from '@/features/blog/domain/blog'
import {
  fromBlogDetailContract,
  toVoteBlogRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function voteBlog(blogId: BlogId, request: VoteBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/vote`, fromBlogDetailContract, toVoteBlogRequestContract(request))
}
