export type UserIdentity = {
  username: string
  displayName: string
  preferences: UserPreferences
}

export type UserDisplayMode = 'display_name' | 'username' | 'display_name_with_username'
export type UserLocale = 'en' | 'zh-CN'
export type ProblemTitleDisplayMode = 'title' | 'slug' | 'title_with_slug'

export type UserPreferences = {
  displayMode: UserDisplayMode
  locale: UserLocale
  problemTitleDisplayMode: ProblemTitleDisplayMode
}

export type AuthUserListItem = {
  username: string
  displayName: string
  email: string
  siteManager: boolean
  problemManager: boolean
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  message: string
}

export type RegisterRequest = {
  username: string
  displayName: string
  email: string
  password: string
}

export type RegisterResponse = LoginResponse

export type SessionResponse = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
}

export type UserProfileResponse = {
  username: string
  displayName: string
  preferences: UserPreferences
  contribution: number
  acceptedProblems: UserAcceptedProblem[]
}

export type UserAcceptedProblem = {
  slug: string
  title: string
  acceptedAt: string
}

export type UserRanklistItem = {
  user: UserIdentity
  contribution: number
}

export type UserAcceptedRanklistItem = {
  user: UserIdentity
  acceptedCount: number
}

export type UpdateUserPermissionsRequest = {
  siteManager: boolean
  problemManager: boolean
}

export type UpdateOwnSettingsRequest = {
  displayName: string
  email: string
  preferences: UserPreferences
  currentPassword: string
  newPassword: string | null
}

export type UpdateManagedUserSettingsRequest = {
  displayName: string
  email: string
  preferences: UserPreferences
  newPassword: string | null
}
