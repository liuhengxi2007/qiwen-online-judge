import { parseUsername } from '@/features/user/lib/user-parsers'
import { parseUserGroupSlug } from '@/features/usergroup/lib/usergroup-parsers'

export const resourceAccessSubjectParsers = {
  parseUsername,
  parseUserGroupSlug,
}
