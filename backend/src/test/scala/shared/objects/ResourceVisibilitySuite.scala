package shared.objects

import munit.FunSuite

class ResourceVisibilitySuite extends FunSuite:

  test("parse maps known visibility values") {
    assertEquals(ResourceVisibility.parse("private"), Right(ResourceVisibility.Private))
    assertEquals(ResourceVisibility.parse("group"), Right(ResourceVisibility.Group))
    assertEquals(ResourceVisibility.parse("public"), Right(ResourceVisibility.Public))
  }

  test("parse rejects unknown values") {
    assertEquals(ResourceVisibility.parse("friends-only"), Left("Resource visibility must be one of: private, group, public."))
  }
