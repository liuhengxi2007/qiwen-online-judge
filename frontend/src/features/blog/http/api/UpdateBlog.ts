import type { BlogDetail } from '@/features/blog/http/response/BlogDetail'
import type { UpdateBlogRequest } from '@/features/blog/http/request/UpdateBlogRequest'
import { blogIdValue } from '@/features/blog/lib/blog-parsers'
import {
  fromBlogDetailContract,
  toUpdateBlogRequestContract,
} from '@/features/blog/http/codec'
import type { BlogId } from '@/features/blog/model/BlogId'
import { postJson } from '@/shared/api/http-client'

export async function updateBlog(blogId: BlogId, request: UpdateBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/update`, fromBlogDetailContract, toUpdateBlogRequestContract(request))
}
