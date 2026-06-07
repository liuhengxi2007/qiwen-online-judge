package domains.submission.api

import cats.effect.IO
import domains.problem.objects.ProblemSlug
import domains.submission.objects.{SubmissionLanguage, SubmissionSourceCode}
import domains.submission.objects.request.CreateSubmissionRequest
import munit.CatsEffectSuite
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Uri}
import shared.api.{HttpApiError, MultipartRequestSupport, PathParams}

import java.nio.charset.StandardCharsets

class CreateSubmissionMultipartDecodeSuite extends CatsEffectSuite:

  test("decodes multipart submission with mixed source parts") {
    val request = MultipartRequestSupport.request(
      List(
        MultipartRequestSupport.textPart("problemSlug", "two-sum"),
        MultipartRequestSupport.textPart(
          "programs",
          """[
            |{"role":"main","language":"cpp17","sourcePart":"source-main"},
            |{"role":"notes.txt","language":"text","sourcePart":"source-notes"}
            |]""".stripMargin
        ),
        MultipartRequestSupport.filePart("source-main", "main.cpp", "int main() {}\n".getBytes(StandardCharsets.UTF_8)),
        MultipartRequestSupport.textPart("source-notes", "42\n")
      ),
      uri = "/api/submissions"
    )

    CreateSubmission(null, null).decode(request, PathParams(Map.empty)).map { decoded =>
      assertEquals(decoded.problemSlug, ProblemSlug("two-sum"))
      assertEquals(decoded.programs("main").language, SubmissionLanguage.Cpp17)
      assertEquals(decoded.programs("main").sourceCode, SubmissionSourceCode("int main() {}\n"))
      assertEquals(decoded.programs("notes.txt").language, SubmissionLanguage.Text)
      assertEquals(decoded.programs("notes.txt").sourceCode, SubmissionSourceCode("42\n"))
    }
  }

  test("decodes JSON submission requests unchanged") {
    val body = CreateSubmissionRequest(
      problemSlug = ProblemSlug("two-sum"),
      programs = Map(
        "main" -> CreateSubmissionRequest.Program(SubmissionLanguage.Cpp17, SubmissionSourceCode("int main() {}\n"))
      )
    )
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString("/api/submissions")).withEntity(body)

    CreateSubmission(null, null).decode(request, PathParams(Map.empty)).map { decoded =>
      assertEquals(decoded, body)
    }
  }

  test("contest submission decode uses the same multipart body shape") {
    val request = MultipartRequestSupport.request(
      List(
        MultipartRequestSupport.textPart("problemSlug", "two-sum"),
        MultipartRequestSupport.textPart("programs", """[{"role":"main","language":"python3","sourcePart":"source-main"}]"""),
        MultipartRequestSupport.filePart("source-main", "main.py", "print('ok')\n".getBytes(StandardCharsets.UTF_8))
      ),
      uri = "/api/contests/spring/submissions"
    )

    CreateContestSubmission(null, null).decode(request, PathParams(Map("contestSlug" -> "spring"))).map { case (contestSlug, decoded) =>
      assertEquals(contestSlug.value, "spring")
      assertEquals(decoded.problemSlug, ProblemSlug("two-sum"))
      assertEquals(decoded.programs("main").language, SubmissionLanguage.Python3)
      assertEquals(decoded.programs("main").sourceCode, SubmissionSourceCode("print('ok')\n"))
    }
  }

  test("rejects invalid UTF-8 in multipart submission source") {
    val request = MultipartRequestSupport.request(
      List(
        MultipartRequestSupport.textPart("problemSlug", "two-sum"),
        MultipartRequestSupport.textPart("programs", """[{"role":"main","language":"cpp17","sourcePart":"source-main"}]"""),
        MultipartRequestSupport.filePart("source-main", "main.cpp", Array(0xc3.toByte, 0x28.toByte))
      ),
      uri = "/api/submissions"
    )

    CreateSubmission(null, null).decode(request, PathParams(Map.empty)).attempt.map {
      case Left(error: HttpApiError) =>
        assertEquals(error.status, Status.BadRequest)
      case other =>
        fail(s"Expected bad request, got $other")
    }
  }
