package shared.objects.access

import io.circe.parser.decode
import munit.FunSuite

class AccessSubjectCodecSuite extends FunSuite:

  test("AccessSubject decoder normalizes user subjects") {
    val result = decode[AccessSubject]("""{"kind":"user","username":"  Alice_01  "}""")

    assertEquals(result, Right(AccessSubject.User(AccessUsername("alice_01"))))
  }

  test("AccessSubject decoder rejects invalid user subjects") {
    val result = decode[AccessSubject]("""{"kind":"user","username":"ab"}""")

    assert(result.left.toOption.exists(_.getMessage.contains("Username must be between 3 and 32 characters.")))
  }
