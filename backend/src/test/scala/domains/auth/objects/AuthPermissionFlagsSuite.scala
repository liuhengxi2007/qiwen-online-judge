package domains.auth.objects

import munit.FunSuite

class AuthPermissionFlagsSuite extends FunSuite:

  test("site manager implies problem and contest manager") {
    assertEquals(
      AuthPermissionFlags.normalize(siteManager = true, problemManager = false, contestManager = false),
      AuthPermissionFlags(siteManager = true, problemManager = true, contestManager = true)
    )
  }

  test("non-site manager keeps explicit manager flags") {
    assertEquals(
      AuthPermissionFlags.normalize(siteManager = false, problemManager = true, contestManager = false),
      AuthPermissionFlags(siteManager = false, problemManager = true, contestManager = false)
    )
  }
