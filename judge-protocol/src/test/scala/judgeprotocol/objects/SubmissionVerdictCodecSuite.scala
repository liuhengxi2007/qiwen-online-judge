package judgeprotocol.objects

import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

class SubmissionVerdictCodecSuite extends FunSuite:

  test("encodes and decodes idleness limit exceeded") {
    assertEquals(SubmissionVerdict.IdlenessLimitExceeded.asJson.noSpaces, "\"idleness_limit_exceeded\"")
    assertEquals(decode[SubmissionVerdict]("\"idleness_limit_exceeded\""), Right(SubmissionVerdict.IdlenessLimitExceeded))
  }
