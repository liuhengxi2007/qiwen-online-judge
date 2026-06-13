import type { Username } from '@/objects/user/Username'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

/** 登录请求体；输入为已解析的用户名和明文密码，响应会建立会话。 */
export type LoginRequest = {
  username: Username
  password: PlaintextPassword
}
