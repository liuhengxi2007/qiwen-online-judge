package domains.user.objects

import io.circe.parser.decode
import munit.FunSuite

class UsernameCodecSuite extends FunSuite:

  test("Username decoder normalizes valid usernames") {
    assertEquals(decode[Username]("\"  Alice_01  \"").map(_.value), Right("alice_01"))
  }

  test("Username decoder rejects invalid usernames") {
    val result = decode[Username]("\"ab\"")

    assert(result.left.toOption.exists(_.getMessage.contains("Username must be between 3 and 32 characters.")))
  }
