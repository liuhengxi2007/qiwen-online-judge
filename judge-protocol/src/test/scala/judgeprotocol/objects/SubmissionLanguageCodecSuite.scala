package judgeprotocol.objects

import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

class SubmissionLanguageCodecSuite extends FunSuite:

  test("encodes and decodes text") {
    assertEquals(SubmissionLanguage.Text.asJson.noSpaces, "\"text\"")
    assertEquals(decode[SubmissionLanguage]("\"text\""), Right(SubmissionLanguage.Text))
  }
