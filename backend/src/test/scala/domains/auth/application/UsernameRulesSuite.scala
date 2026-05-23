package domains.auth.application

import domains.user.model.Username
import munit.FunSuite

class UsernameRulesSuite extends FunSuite:

  test("validate accepts normalized usernames within the supported range") {
    val result = UsernameRules.validate(Username.canonical("  Alice_123  "))

    assertEquals(result, None)
  }

  test("validate accepts usernames at the exact lower and upper bounds") {
    assertEquals(UsernameRules.validate(Username.canonical("abc")), None)
    assertEquals(UsernameRules.validate(Username.canonical("a" * 32)), None)
  }

  test("validate rejects usernames shorter than three characters") {
    val result = UsernameRules.validate(Username.canonical("ab"))

    assertEquals(result, Some("Username must be between 3 and 32 characters."))
  }

  test("validate rejects usernames longer than thirty-two characters") {
    val result = UsernameRules.validate(Username.canonical("a" * 33))

    assertEquals(result, Some("Username must be between 3 and 32 characters."))
  }

  test("validate rejects usernames with unsupported characters") {
    val result = UsernameRules.validate(Username.canonical("alice!"))

    assertEquals(
      result,
      Some("Username may contain only lowercase letters, numbers, underscores, and hyphens.")
    )
  }
