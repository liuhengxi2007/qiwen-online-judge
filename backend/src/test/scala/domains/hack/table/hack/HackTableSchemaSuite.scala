package domains.hack.table.hack

import munit.FunSuite

class HackTableSchemaSuite extends FunSuite:

  test("schema initialization drops legacy hidden hack testcase table") {
    assert(HackTableSchema.dropProblemHackTestcaseTableSql.contains("drop table if exists problem_hack_testcases"))
    assert(!HackTableSchema.initAttemptTableSql.contains("problem_hack_testcases"))
    assert(!HackTableSchema.createIndexesSql.contains("problem_hack_testcases"))
  }
