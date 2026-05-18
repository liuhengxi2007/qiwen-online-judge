import { parseUsername } from '@/features/auth/domain/auth'
import { parseUserGroupSlug } from '@/features/usergroup/domain/usergroup'

export const resourceAccessSubjectParsers = {
  parseUsername,
  parseUserGroupSlug,
}
