import type { RegisteredJudgerListItem } from '@/features/judger/http/response/RegisteredJudgerListItem'
import { fromRegisteredJudgerListItemContract } from '@/features/judger/domain/judger'
import { requestJson } from '@/shared/api/http-client'

export async function listRegisteredJudgers(): Promise<RegisteredJudgerListItem[]> {
  return requestJson('/api/auth/judgers', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid registered judger list payload.')
    }

    return value.map(fromRegisteredJudgerListItemContract)
  })
}
