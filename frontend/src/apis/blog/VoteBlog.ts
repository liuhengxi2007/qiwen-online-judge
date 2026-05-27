import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { VoteBlogRequest } from '@/objects/blog/request/VoteBlogRequest'
import { blogIdValue } from '@/objects/blog/blog-parsers'
import {
  fromBlogDetailContract,
  toVoteBlogRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

export async function voteBlog(blogId: BlogId, request: VoteBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/vote`, fromBlogDetailContract, toVoteBlogRequestContract(request))
}
