package domains.submission.table.submission

import munit.FunSuite

class SubmissionTableSchemaSuite extends FunSuite:

  test("judge derived columns read base result metrics") {
    val metricExpression = "judge_result #>> '{baseResult,score}'"
    val verdictExpression = "judge_result #>> '{baseResult,verdict}'"

    assert(SubmissionTableSchema.initTableSql.contains(metricExpression))
    assert(SubmissionTableSchema.replaceJudgeDerivedColumnsSql.contains(metricExpression))
    assert(SubmissionTableSchema.initTableSql.contains(verdictExpression))
    assert(SubmissionTableSchema.replaceJudgeDerivedColumnsSql.contains(verdictExpression))
    assert(!SubmissionTableSchema.initTableSql.contains("case when judge_result ?"))
    assert(!SubmissionTableSchema.replaceJudgeDerivedColumnsSql.contains("case when judge_result ?"))
  }

  test("scalar backfill writes base and worst result summaries") {
    assert(SubmissionTableSchema.backfillJudgeResultFromLegacySql.contains("'baseResult'"))
    assert(SubmissionTableSchema.backfillJudgeResultFromLegacySql.contains("'worstResult'"))
    assert(SubmissionTableSchema.backfillJudgeResultFromLegacySql.contains("'verdict', verdict"))
  }

  test("legacy JSON backfill writes nested summary verdicts") {
    assert(SubmissionTableSchema.backfillJudgeResultSummaryVerdictsSql.contains("'{baseResult,verdict}'"))
    assert(SubmissionTableSchema.backfillJudgeResultSummaryVerdictsSql.contains("'{worstResult,verdict}'"))
  }
