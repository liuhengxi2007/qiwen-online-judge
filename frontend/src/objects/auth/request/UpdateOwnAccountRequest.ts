import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { PlaintextPassword } from '@/objects/auth/PlaintextPassword'

/** 用户更新自己账号的请求体；需要当前密码确认，可选择设置新密码。 */
export type UpdateOwnAccountRequest = {
  email: EmailAddress
  currentPassword: PlaintextPassword
  newPassword: PlaintextPassword | null
}
