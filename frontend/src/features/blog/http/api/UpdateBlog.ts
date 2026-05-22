import type {
  BlogDetail,
  UpdateBlogRequest,
} from '@/features/blog/domain/blog'
import {
  blogIdValue,
  fromBlogDetailContract,
  toUpdateBlogRequestContract,
} from '@/features/blog/domain/blog'
import type { BlogId } from '@/features/blog/domain/blog'
import { postJson } from '@/shared/api/http-client'

export async function updateBlog(blogId: BlogId, request: UpdateBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/update`, fromBlogDetailContract, toUpdateBlogRequestContract(request))
}
