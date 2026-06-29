import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'

/** 成功 hack 物化为题目数据的内部输入；只供 worker/API mirror 使用。 */
export type MaterializeHackProblemDataInput = {
  problemId: ProblemId
  problemSlug: ProblemSlug
  subtaskIndex: number
  inputPath: ProblemDataPath
  answerPath: ProblemDataPath | null
  testcaseLabel: string
  inputText: string
  answerText: string | null
  createdAt: string
}
