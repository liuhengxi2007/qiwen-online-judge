package judgeprotocol.objects.response

import io.circe.parser.decode
import io.circe.syntax.*
import judgeprotocol.objects.{TestcaseMemoryLimitMb, TestcaseTimeLimitMs}
import munit.FunSuite

class JudgeTaskToolCodecSuite extends FunSuite:

  private val sha256 = "a" * 64
  private val source = JudgeTaskFileRef.unsafe("tools/interactor.cpp", 42L, sha256)
  private val limits = JudgeTaskToolLimits(TestcaseTimeLimitMs(1000), TestcaseMemoryLimitMb(256))

  test("encodes tool source and limits") {
    val json = JudgeTaskTool(source, Some(limits)).asJson
    val cursor = json.hcursor

    assertEquals(cursor.downField("source").get[String]("path"), Right("tools/interactor.cpp"))
    assertEquals(cursor.downField("limits").get[Int]("timeMs"), Right(1000))
    assertEquals(cursor.downField("limits").get[Int]("memoryMb"), Right(256))
  }

  test("decodes interactive mode interactor as a limited tool") {
    val decoded = decode[JudgeTaskMode](
      s"""{
         |  "type":"interactive",
         |  "role":null,
         |  "roles":["main"],
         |  "interactor":{
         |    "source":{"path":"tools/interactor.cpp","sizeBytes":42,"sha256":"$sha256"},
         |    "limits":{"timeMs":1000,"memoryMb":256}
         |  }
         |}""".stripMargin
    )

    assertEquals(decoded.map(_.interactor.flatMap(_.limits).map(_.timeMs.value)), Right(Some(1000)))
    assertEquals(decoded.map(_.interactor.map(_.source.path.value)), Right(Some("tools/interactor.cpp")))
  }

  test("rejects legacy real-time tool limits") {
    val decoded = decode[JudgeTaskTool](
      s"""{
         |  "source":{"path":"tools/tool.cpp","sizeBytes":42,"sha256":"$sha256"},
         |  "limits":{"realTimeMs":1000,"memoryMb":256}
         |}""".stripMargin
    )

    assert(decoded.isLeft)
  }

  test("allows tools without limits") {
    val decoded = decode[JudgeTaskTool](
      s"""{"source":{"path":"tools/tool.cpp","sizeBytes":42,"sha256":"$sha256"}}"""
    )

    assertEquals(decoded.map(_.limits), Right(None))
  }
