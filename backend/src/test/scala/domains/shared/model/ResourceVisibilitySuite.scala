package domains.shared.model

import munit.FunSuite

class ResourceVisibilitySuite extends FunSuite:

  test("fromDatabase maps known visibility values") {
    assertEquals(ResourceVisibility.fromDatabase("private"), Some(ResourceVisibility.Private))
    assertEquals(ResourceVisibility.fromDatabase("group"), Some(ResourceVisibility.Group))
    assertEquals(ResourceVisibility.fromDatabase("public"), Some(ResourceVisibility.Public))
  }

  test("fromDatabase rejects unknown values") {
    assertEquals(ResourceVisibility.fromDatabase("friends-only"), None)
  }

  test("toDatabase returns the persisted representation") {
    assertEquals(ResourceVisibility.toDatabase(ResourceVisibility.Group), "group")
  }
