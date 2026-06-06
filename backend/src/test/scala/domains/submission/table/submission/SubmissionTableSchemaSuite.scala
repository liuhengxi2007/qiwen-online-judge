package domains.submission.table.submission

import munit.FunSuite

class SubmissionTableSchemaSuite extends FunSuite:

  test("judge derived columns read base result metrics") {
    val metricExpression = "judge_result #>> '{baseResult,score}'"

    assert(SubmissionTableSchema.initTableSql.contains(metricExpression))
    assert(SubmissionTableSchema.replaceJudgeDerivedColumnsSql.contains(metricExpression))
    assert(!SubmissionTableSchema.initTableSql.contains("case when judge_result ?"))
    assert(!SubmissionTableSchema.replaceJudgeDerivedColumnsSql.contains("case when judge_result ?"))
  }

  test("scalar backfill writes base and worst result metrics") {
    assert(SubmissionTableSchema.backfillJudgeResultFromLegacySql.contains("'baseResult'"))
    assert(SubmissionTableSchema.backfillJudgeResultFromLegacySql.contains("'worstResult'"))
  }
