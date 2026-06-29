import type { APIMessage } from '@/system/api/api-message'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { UserSearchQuery } from '@/objects/user/request/UserSearchQuery'

/** 查询用户搜索建议；输入已解析搜索词，输出公开用户身份列表。 */
export class ListUserSuggestions implements APIMessage<UserIdentity[]> {
  declare readonly responseType?: UserIdentity[]
  readonly method = 'GET'
  readonly apiPath: string

  constructor(query: UserSearchQuery) {
    const params = new URLSearchParams()
    params.set('q', query)
    this.apiPath = `users/suggestions?${params.toString()}`
  }

  body(): undefined {
    return undefined
  }
}
