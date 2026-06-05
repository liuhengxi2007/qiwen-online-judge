export type AuthPermissionFlags = {
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}

export function normalizeAuthPermissionFlags<T extends AuthPermissionFlags>(flags: T): T {
  return {
    ...flags,
    problemManager: flags.siteManager || flags.problemManager,
    contestManager: flags.siteManager || flags.contestManager,
  }
}
