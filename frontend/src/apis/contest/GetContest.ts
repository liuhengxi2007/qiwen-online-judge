import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { APIMessage } from '@/system/api/api-message'

/** 获取比赛详情；输入公开 slug，输出比赛详情，公开访问边界由后端处理。 */
export class GetContest implements APIMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'GET'
  readonly apiPath: string

  constructor(contestSlug: ContestSlug) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}`
  }

  body(): undefined {
    return undefined
  }
}
