package domains.blog.table.blog_access_grant

import munit.FunSuite

class BlogAccessGrantTableSchemaSuite extends FunSuite:

  test("blog access grants are viewer-only without grant roles") {
    assert(BlogAccessGrantTableSchema.initTableSql.contains("create table if not exists blog_access_grants"))
    assert(!BlogAccessGrantTableSchema.initTableSql.contains("grant_role"))
    assert(!BlogAccessGrantTableSchema.initTableSql.contains("manager"))
    assert(BlogAccessGrantTableSchema.initTableSql.contains("subject_kind in ('user', 'user_group')"))
  }
