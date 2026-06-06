package domains.problem.table.problem

import munit.FunSuite

class ProblemTableSchemaSuite extends FunSuite:

  test("fresh problem schema uses nullable author username") {
    assert(!ProblemTableSchema.initTableSql.contains("creator_username"))
    assert(ProblemTableSchema.initTableSql.contains("author_username varchar(120) references auth_accounts(username) on delete set null"))
    assert(!ProblemTableSchema.initTableSql.contains("author_username varchar(120) not null"))
  }

  test("fresh problem schema defaults submission result display mode to score") {
    assert(ProblemTableSchema.initTableSql.contains("result_display_mode varchar(32) not null default 'score'"))
    assert(ProblemTableSchema.initTableSql.contains("check (result_display_mode in ('verdict', 'score'))"))
  }

  test("problem author migration handles creator and owner columns") {
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("creator_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("owner_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("foreign key (author_username) references auth_accounts(username) on delete set null"))
  }
