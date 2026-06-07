package judgeprotocol.objects.response

import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.{SubmissionLanguage, SubmissionSourceCode}
import munit.FunSuite

class JudgeTaskProgramCodecSuite extends FunSuite:

  private val sha256 = "a" * 64
  private val stub = JudgeTaskFileRef.unsafe("stubs/main.cpp", 42L, sha256)
  private val header = JudgeTaskFileRef.unsafe("headers/xxx.h", 24L, "b" * 64)

  test("encodes optional stub file ref") {
    val json = JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int solve() { return 0; }"), Some(stub)).asJson
    val cursor = json.hcursor

    assertEquals(cursor.get[String]("language"), Right("cpp17"))
    assertEquals(cursor.downField("stub").get[String]("path"), Right("stubs/main.cpp"))
  }

  test("decodes missing stub as none") {
    val decoded = decode[JudgeTaskProgram](
      """{"language":"cpp17","sourceCode":"int main() { return 0; }"}"""
    )

    assertEquals(decoded.map(_.stub), Right(None))
  }

  test("encodes program headers") {
    val json = JudgeTaskProgram(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() { return 0; }"), headers = List(header)).asJson
    val cursor = json.hcursor

    assertEquals(cursor.downField("headers").downArray.get[String]("path"), Right("headers/xxx.h"))
  }

  test("decodes missing headers as empty") {
    val decoded = decode[JudgeTaskProgram](
      """{"language":"cpp17","sourceCode":"int main() { return 0; }"}"""
    )

    assertEquals(decoded.map(_.headers), Right(Nil))
  }
