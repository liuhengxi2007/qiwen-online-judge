import type { BlogDetail } from '@/objects/blog/response/BlogDetail'
import type { UpdateBlogRequest } from '@/objects/blog/request/UpdateBlogRequest'
import { blogIdValue } from '@/objects/blog/BlogId'
import {
  fromBlogDetailContract,
  toUpdateBlogRequestContract,
} from '@/apis/blog/codecs/BlogHttpCodecs'
import type { BlogId } from '@/objects/blog/BlogId'
import { postJson } from '@/system/api/http-client'

export async function updateBlog(blogId: BlogId, request: UpdateBlogRequest): Promise<BlogDetail> {
  return postJson(`/api/blogs/${blogIdValue(blogId)}/update`, fromBlogDetailContract, toUpdateBlogRequestContract(request))
}
