package domains.hack.api

import cats.effect.IO
import domains.hack.objects.request.CreateHackRequest
import domains.submission.objects.SubmissionId
import munit.CatsEffectSuite
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Uri}
import shared.api.{HttpApiError, MultipartRequestSupport, PathParams}

import java.nio.charset.StandardCharsets

class HackApiSupportSuite extends CatsEffectSuite:

  test("normalizes CRLF, CR, trailing spaces and tabs, and final LF") {
    assertEquals(
      HackApiSupport.normalizeHackInput("1  \r\n2\t\r3"),
      "1\n2\n3\n"
    )
  }

  test("normalizes empty input and all-whitespace input") {
    assertEquals(HackApiSupport.normalizeHackInput(""), "")
    assertEquals(HackApiSupport.normalizeHackInput(" \t"), "\n")
  }

  test("normalization is idempotent") {
    val normalized = HackApiSupport.normalizeHackInput("1  \r\n\n2\t")
    assertEquals(HackApiSupport.normalizeHackInput(normalized), normalized)
  }

  test("rejects hack input over 10 MiB after normalization") {
    val input = "x" * (HackApiSupport.MaxHackInputChars + 1)
    HackApiSupport.validateHackText(input, None, requiresStrategyProvider = false).attempt.map {
      case Left(error: HttpApiError) =>
        assertEquals(error.status, Status.BadRequest)
        assertEquals(error.fallbackMessage, Some(s"Hack input must be at most ${HackApiSupport.MaxHackInputChars} characters."))
      case other =>
        fail(s"Expected bad request, got $other")
    }
  }

  test("decodes JSON create hack requests unchanged") {
    val body = CreateHackRequest(SubmissionId(7), subtaskIndex = 2, input = "1  \r\n", strategyProviderSource = Some("int main() {}\n"))
    val request = Request[IO](method = Method.POST, uri = uri("/api/hacks")).withEntity(body)

    CreateHack(null, null).decode(request, PathParams(Map.empty)).map { decoded =>
      assertEquals(decoded, body)
    }
  }

  test("decodes multipart create hack with file parts") {
    val request = MultipartRequestSupport.request(
      List(
        MultipartRequestSupport.textPart("targetSubmissionId", "42"),
        MultipartRequestSupport.textPart("subtaskIndex", "3"),
        MultipartRequestSupport.filePart("inputFile", "case.in", "1  \r\n2\t".getBytes(StandardCharsets.UTF_8)),
        MultipartRequestSupport.filePart("strategyProviderFile", "strategy.cpp", "int main() {}\n".getBytes(StandardCharsets.UTF_8))
      ),
      uri = "/api/hacks"
    )

    CreateHack(null, null).decode(request, PathParams(Map.empty)).map { decoded =>
      assertEquals(decoded.targetSubmissionId, SubmissionId(42))
      assertEquals(decoded.subtaskIndex, 3)
      assertEquals(decoded.input, "1  \r\n2\t")
      assertEquals(decoded.strategyProviderSource, Some("int main() {}\n"))
    }
  }

  test("rejects invalid UTF-8 in multipart hack input") {
    val request = MultipartRequestSupport.request(
      List(
        MultipartRequestSupport.textPart("targetSubmissionId", "42"),
        MultipartRequestSupport.textPart("subtaskIndex", "3"),
        MultipartRequestSupport.filePart("inputFile", "case.in", Array(0xc3.toByte, 0x28.toByte))
      ),
      uri = "/api/hacks"
    )

    CreateHack(null, null).decode(request, PathParams(Map.empty)).attempt.map {
      case Left(error: HttpApiError) =>
        assertEquals(error.status, Status.BadRequest)
      case other =>
        fail(s"Expected bad request, got $other")
    }
  }

  private def uri(value: String): Uri =
    Uri.fromString(value).fold(error => fail(error.toString), identity)
