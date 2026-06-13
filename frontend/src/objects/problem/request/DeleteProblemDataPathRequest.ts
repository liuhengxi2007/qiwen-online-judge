import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'

/** 删除题目数据路径请求体；path 必须是已校验的相对数据路径。 */
export type DeleteProblemDataPathRequest = {
  path: ProblemDataPath
}
