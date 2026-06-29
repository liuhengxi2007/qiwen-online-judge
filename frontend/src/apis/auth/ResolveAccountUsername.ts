import type { APIMessage } from '@/system/api/api-message'
import type { ResolveAccountUsernameResponse } from '@/objects/auth/response/ResolveAccountUsernameResponse'
import type { Username } from '@/objects/user/Username'

/** 内部账号用户名解析请求体；用于不通过 URL 暴露查询参数的账号存在性检查。 */
type ResolveAccountUsernameBody = {
  username: Username
}

/** 解析账号用户名的内部 API；输入候选用户名，输出匹配账号或空值。 */
export class ResolveAccountUsername implements APIMessage<ResolveAccountUsernameResponse> {
  declare readonly responseType?: ResolveAccountUsernameResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/auth/resolve-account-username'
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
  }

  body(): ResolveAccountUsernameBody {
    return { username: this.username }
  }
}
