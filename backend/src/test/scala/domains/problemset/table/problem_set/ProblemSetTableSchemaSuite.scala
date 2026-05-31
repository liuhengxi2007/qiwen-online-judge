package domains.problemset.table.problem_set

import munit.FunSuite

class ProblemSetTableSchemaSuite extends FunSuite:

  test("fresh problem set schema uses restricted base access without visibility") {
    assert(!ProblemSetTableSchema.initTableSql.contains("visibility"))
    assert(ProblemSetTableSchema.initTableSql.contains("default 'restricted'"))
    assert(ProblemSetTableSchema.initTableSql.contains("check (base_access in ('restricted', 'public'))"))
  }
