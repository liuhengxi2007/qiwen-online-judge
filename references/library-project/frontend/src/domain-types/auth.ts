export const AdminAccountStatuses = {
  Active: 'active',
  SessionExpired: 'session_expired',
  NotFound: 'not_found',
  PasswordInvalid: 'password_invalid',
} as const

export type AdminAccountStatus =
  (typeof AdminAccountStatuses)[keyof typeof AdminAccountStatuses]

export class AdminCredentials {
  readonly username: string
  readonly password: string

  constructor(username: string, password: string) {
    this.username = username
    this.password = password
  }

  static fromForm(username: string, password: string) {
    return new AdminCredentials(username.trim(), password.trim())
  }

  get isComplete() {
    return this.username.length > 0 && this.password.length > 0
  }
}

export class AdminRegistrationDraft {
  readonly username: string
  readonly password: string
  readonly confirmPassword: string

  constructor(username: string, password: string, confirmPassword: string) {
    this.username = username
    this.password = password
    this.confirmPassword = confirmPassword
  }

  static fromForm(username: string, password: string, confirmPassword: string) {
    return new AdminRegistrationDraft(username.trim(), password.trim(), confirmPassword.trim())
  }

  get isComplete() {
    return this.username.length > 0 && this.password.length > 0 && this.confirmPassword.length > 0
  }

  get passwordsMatch() {
    return this.password === this.confirmPassword
  }
}

export interface AdminLoginRequest {
  credentials: AdminCredentials
}

export interface AdminRegisterRequest {
  draft: AdminRegistrationDraft
}

export interface AdminAuthSuccessPayload {
  accountStatus: typeof AdminAccountStatuses.Active
  redirectTo: string
}

export interface AdminAuthFailurePayload {
  accountStatus:
    | typeof AdminAccountStatuses.SessionExpired
    | typeof AdminAccountStatuses.NotFound
    | typeof AdminAccountStatuses.PasswordInvalid
  reason: string
}

export type AdminLoginResponse = AdminAuthSuccessPayload | AdminAuthFailurePayload

export type AdminRegisterResponse =
  | {
      accountStatus: typeof AdminAccountStatuses.Active
      redirectTo: string
    }
  | {
      accountStatus:
        | typeof AdminAccountStatuses.NotFound
        | typeof AdminAccountStatuses.PasswordInvalid
      reason: string
    }
