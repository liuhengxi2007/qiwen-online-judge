import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { VoteBlogRequest } from '@/features/blog/http/request/VoteBlogRequest'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toVoteBlogRequestContract,
} from '@/features/blog/http/codec/BlogHttpCodecs'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'

export async function voteBlog(blogId: BlogId, request: VoteBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/vote`, fromBlogDetailContract, toVoteBlogRequestContract(request))
}
