package domains.problemset.table.problem_set

import munit.FunSuite

class ProblemSetTableSchemaSuite extends FunSuite:

  test("fresh problem set schema uses restricted base access without visibility") {
    assert(!ProblemSetTableSchema.initTableSql.contains("visibility"))
    assert(ProblemSetTableSchema.initTableSql.contains("default 'restricted'"))
    assert(ProblemSetTableSchema.initTableSql.contains("check (base_access in ('restricted', 'public'))"))
  }

  test("fresh problem set schema uses nullable author username") {
    assert(!ProblemSetTableSchema.initTableSql.contains("creator_username"))
    assert(ProblemSetTableSchema.initTableSql.contains("author_username varchar(120) references auth_accounts(username) on delete set null"))
    assert(!ProblemSetTableSchema.initTableSql.contains("author_username varchar(120) not null"))
  }

  test("problem set author migration handles creator and owner columns") {
    assert(ProblemSetTableSchema.migrateAuthorUsernameColumnSql.contains("creator_username"))
    assert(ProblemSetTableSchema.migrateAuthorUsernameColumnSql.contains("owner_username"))
    assert(ProblemSetTableSchema.migrateAuthorUsernameColumnSql.contains("foreign key (author_username) references auth_accounts(username) on delete set null"))
  }
