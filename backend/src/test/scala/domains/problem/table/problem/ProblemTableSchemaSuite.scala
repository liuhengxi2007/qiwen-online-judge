package domains.problem.table.problem

import munit.FunSuite

class ProblemTableSchemaSuite extends FunSuite:

  test("fresh problem schema uses nullable author username") {
    assert(!ProblemTableSchema.initTableSql.contains("creator_username"))
    assert(ProblemTableSchema.initTableSql.contains("author_username varchar(120) references auth_accounts(username) on delete set null"))
    assert(!ProblemTableSchema.initTableSql.contains("author_username varchar(120) not null"))
  }

  test("problem author migration handles creator and owner columns") {
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("creator_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("owner_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("foreign key (author_username) references auth_accounts(username) on delete set null"))
  }
