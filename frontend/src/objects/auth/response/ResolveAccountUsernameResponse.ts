import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import { readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ResolveAccountUsernameResponse = {
  username: Username | null
}

export function fromResolveAccountUsernameResponseContract(
  value: unknown,
  label = 'resolve account username response',
): ResolveAccountUsernameResponse {
  const response = readRecord(value, label)
  return {
    username: readNullable(response.username, `${label} username`, (username, usernameLabel) =>
      fromUsernameContract(readString(username, usernameLabel), usernameLabel),
    ),
  }
}
