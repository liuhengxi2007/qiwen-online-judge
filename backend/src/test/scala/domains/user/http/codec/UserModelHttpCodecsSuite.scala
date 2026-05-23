package domains.user.http.codec

import domains.user.http.codec.UserModelHttpCodecs.given
import domains.user.model.Username
import io.circe.parser.decode
import munit.FunSuite

class UserModelHttpCodecsSuite extends FunSuite:

  test("Username decoder normalizes valid usernames") {
    assertEquals(decode[Username]("\"  Alice_01  \"").map(_.value), Right("alice_01"))
  }

  test("Username decoder rejects invalid usernames") {
    val result = decode[Username]("\"ab\"")

    assert(result.left.toOption.exists(_.getMessage.contains("Username must be between 3 and 32 characters.")))
  }
