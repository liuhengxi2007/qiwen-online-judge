package judgeprotocol.objects.response

import io.circe.parser.decode
import munit.FunSuite

class JudgeTaskTestcaseCodecSuite extends FunSuite:

  private val fileSha = "a" * 64
  private val baseJson =
    s"""
       |{
       |  "index":1,
       |  "label":null,
       |  "testcaseType":"main",
       |  "scoreRatio":1,
       |  "limits":{"timeMs":1000,"memoryMb":256},
       |  "checker":{"type":"builtin","name":"exact","source":null},
       |  "input":{"path":"tests/1.in","sizeBytes":1,"sha256":"$fileSha"},
       |  "answer":{"path":"tests/1.ans","sizeBytes":1,"sha256":"$fileSha"},
       |  "strategyProvider":null
       |}
       |""".stripMargin

  test("decodes missing testcase roles as empty list") {
    assertEquals(decode[JudgeTaskTestcase](baseJson).map(_.roles), Right(Nil))
  }

  test("decodes testcase roles") {
    val json = baseJson.replace(""""strategyProvider":null""", """"strategyProvider":null,"roles":["chain.txt","main"]""")

    assertEquals(decode[JudgeTaskTestcase](json).map(_.roles), Right(List("chain.txt", "main")))
  }
