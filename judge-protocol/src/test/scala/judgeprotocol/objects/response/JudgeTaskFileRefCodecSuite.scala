package judgeprotocol.objects.response

import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

class JudgeTaskFileRefCodecSuite extends FunSuite:

  private val sha256 = "a" * 64

  test("encodes as the existing wire field shape") {
    val json = JudgeTaskFileRef.unsafe("cases/1.in", 42L, sha256).asJson
    val cursor = json.hcursor

    assertEquals(cursor.get[String]("path"), Right("cases/1.in"))
    assertEquals(cursor.get[Long]("sizeBytes"), Right(42L))
    assertEquals(cursor.get[String]("sha256"), Right(sha256))
  }

  test("decodes valid relative file refs") {
    val decoded = decode[JudgeTaskFileRef](s"""{"path":"cases/1.in","sizeBytes":42,"sha256":"$sha256"}""")

    assertEquals(decoded.map(_.path.value), Right("cases/1.in"))
    assertEquals(decoded.map(_.sizeBytes.value), Right(42L))
    assertEquals(decoded.map(_.sha256.value), Right(sha256))
  }

  test("rejects escaping paths") {
    val decoded = decode[JudgeTaskFileRef](s"""{"path":"../secret","sizeBytes":42,"sha256":"$sha256"}""")

    assert(decoded.isLeft)
  }

  test("rejects negative size") {
    val decoded = decode[JudgeTaskFileRef](s"""{"path":"cases/1.in","sizeBytes":-1,"sha256":"$sha256"}""")

    assert(decoded.isLeft)
  }

  test("rejects invalid sha256") {
    val decoded = decode[JudgeTaskFileRef]("""{"path":"cases/1.in","sizeBytes":42,"sha256":"not-a-sha"}""")

    assert(decoded.isLeft)
  }
