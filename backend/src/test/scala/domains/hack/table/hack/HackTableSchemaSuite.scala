package domains.hack.table.hack

import munit.FunSuite

class HackTableSchemaSuite extends FunSuite:

  test("schema initialization drops legacy hidden hack testcase table") {
    assert(HackTableSchema.dropProblemHackTestcaseTableSql.contains("drop table if exists problem_hack_testcases"))
    assert(!HackTableSchema.initAttemptTableSql.contains("problem_hack_testcases"))
    assert(!HackTableSchema.createIndexesSql.contains("problem_hack_testcases"))
  }

  test("schema keeps hack judge result snapshot on attempts") {
    assert(HackTableSchema.initAttemptTableSql.contains("judge_result jsonb"))
    assert(HackTableSchema.addJudgeResultColumnSql.contains("add column if not exists judge_result jsonb"))
    assert(!HackTableSchema.initAttemptTableSql.contains("new_score"))
    assert(HackTableSchema.dropNewScoreColumnSql.contains("drop column if exists new_score"))
  }
