import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'

/** 获取用户已通过题目分页列表；输入用户名和页码，输出当前页题目。 */
export class ListUserAcceptedProblems implements APIWithSessionMessage<PageResponse<UserAcceptedProblem>> {
  declare readonly responseType?: PageResponse<UserAcceptedProblem>
  readonly method = 'GET'
  readonly apiPath: string

  constructor(username: Username, page: number) {
    this.apiPath = `users/${encodeURIComponent(usernameValue(username))}/accepted-problems?page=${encodeURIComponent(String(page))}`
  }

  body(): undefined {
    return undefined
  }
}
