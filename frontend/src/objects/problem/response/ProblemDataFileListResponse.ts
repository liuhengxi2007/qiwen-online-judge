import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'

/** 题目数据文件名列表响应；仅返回当前目录/数据包可见的文件名集合。 */
export type ProblemDataFileListResponse = {
  items: ProblemDataFilename[]
}
