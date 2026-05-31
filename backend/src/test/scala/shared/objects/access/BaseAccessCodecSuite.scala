package shared.objects.access

import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

class BaseAccessCodecSuite extends FunSuite:

  test("BaseAccess decoder maps legacy owner_only to Restricted") {
    assertEquals(decode[BaseAccess]("\"owner_only\""), Right(BaseAccess.Restricted))
  }

  test("BaseAccess encoder emits restricted") {
    assertEquals(BaseAccess.Restricted.asJson.noSpaces, "\"restricted\"")
  }
