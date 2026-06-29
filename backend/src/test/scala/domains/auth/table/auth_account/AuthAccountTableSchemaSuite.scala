package domains.auth.table.auth_account

import munit.FunSuite

class AuthAccountTableSchemaSuite extends FunSuite:

  test("site manager permission invariant is backfilled and constrained") {
    assert(AuthAccountTableSchema.backfillSiteManagerPermissionFlagsSql.contains("where site_manager = true"))
    assert(AuthAccountTableSchema.backfillSiteManagerPermissionFlagsSql.contains("problem_manager = true"))
    assert(AuthAccountTableSchema.backfillSiteManagerPermissionFlagsSql.contains("contest_manager = true"))
    assert(AuthAccountTableSchema.addSiteManagerPermissionFlagsConstraintSql.contains("not site_manager or (problem_manager and contest_manager)"))
  }
