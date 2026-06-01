import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { fromUserGroupSlugContract } from '@/objects/usergroup/UserGroupSlug'
import { readArray, readRecord, readString } from '@/objects/shared/PageResponse'

export type UserGroupSlugListResponse = {
  slugs: UserGroupSlug[]
}

export function fromUserGroupSlugListResponseContract(
  value: unknown,
  label = 'user group slug list response',
): UserGroupSlugListResponse {
  const response = readRecord(value, label)
  return {
    slugs: readArray(response.slugs, `${label} slugs`, (slug, slugLabel) =>
      fromUserGroupSlugContract(readString(slug, slugLabel), slugLabel),
    ),
  }
}
