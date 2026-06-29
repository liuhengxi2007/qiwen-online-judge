import { parseEmailAddress } from '@/objects/auth/EmailAddress'
import { parsePlaintextPassword } from '@/objects/auth/PlaintextPassword'
import type { UpdateManagedUserAccountRequest } from '@/objects/auth/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/objects/auth/request/UpdateOwnAccountRequest'

/**
 * 用户账户设置草稿，保存邮箱、旧密码和新密码输入。
 */
export type UserAccountDraft = {
  email: string
  currentPassword: string
  newPassword: string
  confirmNewPassword: string
}

/**
 * 用户设置校验成功结果，携带可提交请求体。
 */
type ValidationSuccess<T> = {
  ok: true
  request: T
}

/**
 * 用户设置校验失败结果，携带用户可见错误。
 */
type ValidationFailure = {
  ok: false
  message: string
}

/**
 * 用户账户校验消息集合，由页面传入本地化文案。
 */
type UserAccountValidationMessages = {
  confirmNewPassword: string
  passwordMismatch: string
}

/**
 * 校验用户账户设置草稿；支持只改邮箱、只改密码或二者同时修改。
 */
export function validateUserAccountDraft(
  draft: UserAccountDraft,
  isEditingOwnSettings: boolean,
  messages: UserAccountValidationMessages,
): ValidationSuccess<UpdateOwnAccountRequest | UpdateManagedUserAccountRequest> | ValidationFailure {
  const emailResult = parseEmailAddress(draft.email)
  if (!emailResult.ok) {
    return { ok: false, message: emailResult.error }
  }

  const newPasswordResult = draft.newPassword.trim() ? parsePlaintextPassword(draft.newPassword) : null
  if (newPasswordResult && !newPasswordResult.ok) {
    return { ok: false, message: newPasswordResult.error }
  }

  const confirmNewPasswordResult = draft.confirmNewPassword.trim()
    ? parsePlaintextPassword(draft.confirmNewPassword)
    : null
  if (confirmNewPasswordResult && !confirmNewPasswordResult.ok) {
    return { ok: false, message: confirmNewPasswordResult.error }
  }

  if (newPasswordResult || confirmNewPasswordResult) {
    if (
      !newPasswordResult ||
      !confirmNewPasswordResult ||
      newPasswordResult.value !== confirmNewPasswordResult.value
    ) {
      return {
        ok: false,
        message: confirmNewPasswordResult?.ok ? messages.passwordMismatch : messages.confirmNewPassword,
      }
    }
  }

  if (isEditingOwnSettings) {
    const currentPasswordResult = parsePlaintextPassword(draft.currentPassword)
    if (!currentPasswordResult.ok) {
      return { ok: false, message: currentPasswordResult.error }
    }

    return {
      ok: true,
      request: {
        email: emailResult.value,
        currentPassword: currentPasswordResult.value,
        newPassword: newPasswordResult ? newPasswordResult.value : null,
      },
    }
  }

  return {
    ok: true,
    request: {
      email: emailResult.value,
      newPassword: newPasswordResult ? newPasswordResult.value : null,
    },
  }
}
