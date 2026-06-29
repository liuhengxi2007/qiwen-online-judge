import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

/** 管理员更新账号请求体；允许不提供新密码，权限由后端会话校验。 */
export type UpdateManagedUserAccountRequest = {
  email: EmailAddress
  newPassword: PlaintextPassword | null
}
