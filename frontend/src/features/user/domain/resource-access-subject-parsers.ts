import { parseUsername } from '@/features/user/domain/user'
import { parseUserGroupSlug } from '@/features/usergroup/domain/usergroup'

export const resourceAccessSubjectParsers = {
  parseUsername,
  parseUserGroupSlug,
}
