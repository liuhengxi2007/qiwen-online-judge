package domains.blog.table.blog

import munit.FunSuite

class BlogTableSupportSuite extends FunSuite:

  test("blog visibility predicate covers author public user and group grants") {
    val predicate = BlogTableSupport.blogVisibleToViewerPredicate("b")

    assert(predicate.contains("b.author_username = ?"))
    assert(predicate.contains("b.base_access = 'public'"))
    assert(predicate.contains("from blog_access_grants bag"))
    assert(predicate.contains("bag.subject_kind = 'user'"))
    assert(predicate.contains("bag.subject_kind = 'user_group'"))
    assert(predicate.contains("join user_group_memberships ugm"))
  }
