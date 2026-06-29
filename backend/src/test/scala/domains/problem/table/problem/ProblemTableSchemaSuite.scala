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

  test("problem schema uses rejudge revision and migrates legacy hack revision") {
    assert(ProblemTableSchema.initTableSql.contains("rejudge_revision bigint not null default 0"))
    assert(!ProblemTableSchema.initTableSql.contains("hack_revision"))
    assert(ProblemTableSchema.addRejudgeRevisionColumnSql.contains("rename column hack_revision to rejudge_revision"))
    assert(ProblemTableSchema.addRejudgeRevisionColumnSql.contains("greatest(coalesce(rejudge_revision, 0), coalesce(hack_revision, 0))"))
    assert(ProblemTableSchema.addRejudgeRevisionColumnSql.contains("drop column hack_revision"))
    assert(ProblemTableSchema.setRejudgeRevisionDefaultSql.contains("alter column rejudge_revision set default 0"))
    assert(ProblemTableSchema.setRejudgeRevisionNotNullSql.contains("alter column rejudge_revision set not null"))
  }

  test("problem author migration handles creator and owner columns") {
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("creator_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("owner_username"))
    assert(ProblemTableSchema.migrateAuthorUsernameColumnSql.contains("foreign key (author_username) references auth_accounts(username) on delete set null"))
  }
