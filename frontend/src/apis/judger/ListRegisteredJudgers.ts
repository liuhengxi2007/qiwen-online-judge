import type { RegisteredJudgerListItem } from '@/objects/judger/response/RegisteredJudgerListItem'
import { fromRegisteredJudgerListItemContract } from '@/apis/judger/codecs/JudgerRegistryHttpCodecs'
import { requestJson } from '@/system/api/http-client'

export async function listRegisteredJudgers(): Promise<RegisteredJudgerListItem[]> {
  return requestJson('/api/judgers', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid registered judger list payload.')
    }

    return value.map(fromRegisteredJudgerListItemContract)
  })
}
