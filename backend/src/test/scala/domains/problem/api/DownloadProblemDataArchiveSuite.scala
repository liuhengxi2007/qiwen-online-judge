package domains.problem.api

import cats.effect.IO
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifestEntry
import munit.CatsEffectSuite
import org.http4s.Status
import org.typelevel.ci.CIString
import shared.api.HttpApiError

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class DownloadProblemDataArchiveSuite extends CatsEffectSuite:

  private val problemSlug = ProblemSlug("two-sum")

  test("writes zip entries with original relative paths and contents") {
    val files = Map(
      path("judge.yaml") -> utf8("checker: diff\n"),
      path("cases/1.in") -> utf8("1 2\n"),
      path("cases/1.out") -> utf8("3\n")
    )

    DownloadProblemDataArchive.archiveResponse(
      problemSlug,
      List(entry("cases/1.out"), entry("judge.yaml"), entry("cases/1.in")),
      fixedReadPath(files)
    ).flatMap { response =>
      response.body.compile.to(Array).map { bytes =>
        assertEquals(response.status, Status.Ok)
        assertEquals(response.headers.get(CIString("Content-Type")).map(_.head.value), Some("application/zip"))
        assertEquals(
          response.headers.get(CIString("Content-Disposition")).map(_.head.value),
          Some("""attachment; filename="two-sum-data.zip"""")
        )
        assertEquals(response.headers.get(CIString("Content-Length")).map(_.head.value), Some(bytes.length.toString))
        assertEquals(
          unzipUtf8(bytes),
          List(
            "cases/1.in" -> "1 2\n",
            "cases/1.out" -> "3\n",
            "judge.yaml" -> "checker: diff\n"
          )
        )
      }
    }
  }

  test("writes a readable empty zip when manifest has no entries") {
    DownloadProblemDataArchive.archiveResponse(problemSlug, List.empty, fixedReadPath(Map.empty)).flatMap { response =>
      response.body.compile.to(Array).map { bytes =>
        assertEquals(response.status, Status.Ok)
        assertEquals(unzipUtf8(bytes), List.empty)
      }
    }
  }

  test("returns internal error when manifest references missing stored bytes") {
    DownloadProblemDataArchive.archiveResponse(problemSlug, List(entry("judge.yaml")), fixedReadPath(Map.empty)).attempt.map {
      case Left(error: HttpApiError) =>
        assertEquals(error.status, Status.InternalServerError)
      case other =>
        fail(s"Expected HttpApiError, got $other")
    }
  }

  private def fixedReadPath(files: Map[ProblemDataPath, Array[Byte]]): DownloadProblemDataArchive.ReadPath =
    (slug, path) =>
      val _ = slug
      IO.pure(files.get(path).map(bytes => path -> bytes))

  private def path(value: String): ProblemDataPath =
    ProblemDataPath(value)

  private def entry(value: String): ProblemDataManifestEntry =
    ProblemDataManifestEntry(path(value), sizeBytes = 1L, sha256 = "0" * 64)

  private def utf8(value: String): Array[Byte] =
    value.getBytes(StandardCharsets.UTF_8)

  private def unzipUtf8(bytes: Array[Byte]): List[(String, String)] =
    val zip = ZipInputStream(ByteArrayInputStream(bytes))
    try
      Iterator
        .continually(zip.getNextEntry)
        .takeWhile(_ != null)
        .map { entry =>
          val name = entry.getName
          val content = new String(zip.readAllBytes(), StandardCharsets.UTF_8)
          zip.closeEntry()
          name -> content
        }
        .toList
    finally zip.close()
