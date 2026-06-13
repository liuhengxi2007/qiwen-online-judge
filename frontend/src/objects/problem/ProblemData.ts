import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'

/** 题目数据入口信息；value 为空表示尚未上传或未绑定数据包。 */
export type ProblemData = {
  value: ProblemDataFilename | null
}
