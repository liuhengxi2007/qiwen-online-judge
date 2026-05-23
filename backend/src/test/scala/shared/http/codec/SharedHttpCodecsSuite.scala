package shared.http.codec

import domains.user.model.Username
import io.circe.parser.decode
import munit.FunSuite
import shared.access.AccessSubject
import shared.http.codec.SharedHttpCodecs.given

class SharedHttpCodecsSuite extends FunSuite:

  test("AccessSubject decoder normalizes user subjects") {
    val result = decode[AccessSubject]("""{"kind":"user","username":"  Alice_01  "}""")

    assertEquals(result, Right(AccessSubject.User(Username("alice_01"))))
  }

  test("AccessSubject decoder rejects invalid user subjects") {
    val result = decode[AccessSubject]("""{"kind":"user","username":"ab"}""")

    assert(result.left.toOption.exists(_.getMessage.contains("Username must be between 3 and 32 characters.")))
  }
