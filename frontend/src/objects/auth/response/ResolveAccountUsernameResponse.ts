import type { Username } from '@/objects/user/Username'

/** 内部账号用户名解析响应；username 为空表示目标账号不存在或不可用。 */
export type ResolveAccountUsernameResponse = {
  username: Username | null
}
