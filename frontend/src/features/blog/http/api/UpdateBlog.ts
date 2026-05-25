import type { BlogDetail } from '@/features/blog/model/response/BlogDetail'
import type { UpdateBlogRequest } from '@/features/blog/model/request/UpdateBlogRequest'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toUpdateBlogRequestContract,
} from '@/features/blog/http/codec/BlogHttpCodecs'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'

export async function updateBlog(blogId: BlogId, request: UpdateBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/update`, fromBlogDetailContract, toUpdateBlogRequestContract(request))
}
