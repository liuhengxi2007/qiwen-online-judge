import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

/** 注册请求体；包含新账号基础资料和明文密码，调用后由后端创建账号。 */
export type RegisterRequest = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
}
