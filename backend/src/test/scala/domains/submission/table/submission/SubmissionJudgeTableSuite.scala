package domains.submission.table.submission

import munit.FunSuite

class SubmissionJudgeTableSuite extends FunSuite:

  test("judge queue priorities keep manual problem rejudge between ordinary and hack rejudge") {
    assertEquals(SubmissionJudgeTable.OrdinaryPriority, 0)
    assert(SubmissionJudgeTable.OrdinaryPriority > SubmissionJudgeTable.ManualProblemRejudgePriority)
    assert(SubmissionJudgeTable.ManualProblemRejudgePriority > SubmissionJudgeTable.LowPriority)
  }
