package domains.blog.table.blog

import munit.FunSuite

class BlogTableSchemaSuite extends FunSuite:

  test("fresh blog schema uses base access without visibility") {
    assert(!BlogTableSchema.initTableSql.contains("visibility"))
    assert(BlogTableSchema.initTableSql.contains("base_access varchar(32) not null default 'public'"))
    assert(BlogTableSchema.initTableSql.contains("check (base_access in ('restricted', 'public'))"))
  }

  test("blog base access migration maps legacy private visibility to restricted") {
    assert(BlogTableSchema.addBaseAccessColumnSql.contains("when visibility = 'public' then 'public'"))
    assert(BlogTableSchema.addBaseAccessColumnSql.contains("else 'restricted'"))
    assert(BlogTableSchema.dropVisibilityColumnSql.contains("drop column if exists visibility"))
  }
