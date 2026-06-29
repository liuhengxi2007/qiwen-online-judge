import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'

/** 提交来源；为空表示普通题目提交，非空表示来自某场比赛。 */
export type SubmissionSource = {
  contestSlug: ContestSlug | null
  contestTitle: ContestTitle | null
}
