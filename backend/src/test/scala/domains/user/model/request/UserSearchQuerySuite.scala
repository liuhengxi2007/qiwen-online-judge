package domains.user.model.request

import munit.FunSuite

class UserSearchQuerySuite extends FunSuite:

  test("parse trims valid queries") {
    val result = UserSearchQuery.parse("  alice  ")

    assertEquals(result, Right(UserSearchQuery("alice")))
  }

  test("parse rejects blank queries") {
    val result = UserSearchQuery.parse("   ")

    assertEquals(result, Left("User search query is required."))
  }
