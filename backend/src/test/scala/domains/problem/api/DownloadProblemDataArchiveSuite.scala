package domains.problem.api

import cats.effect.IO
import domains.problem.objects.{ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.{ProblemDataManifest, ProblemDataManifestEntry}
import domains.problem.utils.ProblemDataStorage
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
    val storage = FixedProblemDataStorage(
      Map(
        path("judge.yaml") -> utf8("checker: diff\n"),
        path("cases/1.in") -> utf8("1 2\n"),
        path("cases/1.out") -> utf8("3\n")
      )
    )
    val api = DownloadProblemDataArchive(storage)

    api.archiveResponse(
      problemSlug,
      List(entry("cases/1.out"), entry("judge.yaml"), entry("cases/1.in"))
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
    val api = DownloadProblemDataArchive(FixedProblemDataStorage(Map.empty))

    api.archiveResponse(problemSlug, List.empty).flatMap { response =>
      response.body.compile.to(Array).map { bytes =>
        assertEquals(response.status, Status.Ok)
        assertEquals(unzipUtf8(bytes), List.empty)
      }
    }
  }

  test("returns internal error when manifest references missing stored bytes") {
    val api = DownloadProblemDataArchive(FixedProblemDataStorage(Map.empty))

    api.archiveResponse(problemSlug, List(entry("judge.yaml"))).attempt.map {
      case Left(error: HttpApiError) =>
        assertEquals(error.status, Status.InternalServerError)
      case other =>
        fail(s"Expected HttpApiError, got $other")
    }
  }

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

  private final class FixedProblemDataStorage(files: Map[ProblemDataPath, Array[Byte]]) extends ProblemDataStorage:
    override def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]] =
      val _ = problemSlug
      IO.pure(files.keys.toList.sortBy(_.value))

    override def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest] =
      IO.pure(
        ProblemDataManifest.fromEntries(
          problemSlug,
          files.toList.map { case (path, bytes) =>
            ProblemDataManifestEntry(path, sizeBytes = bytes.length.toLong, sha256 = "0" * 64)
          }
        )
      )

    override def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataStorage.ProblemDataSnapshot] =
      val _ = problemSlug
      IO.pure(files)

    override def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath] =
      val _ = (problemSlug, bytes)
      IO.pure(path)

    override def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]] =
      val _ = problemSlug
      IO.pure(files.get(path).map(bytes => path -> bytes))

    override def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean] =
      val _ = (problemSlug, path)
      IO.pure(false)

    override def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit] =
      val _ = problemSlug
      IO.unit

    override def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataStorage.ProblemDataSnapshot): IO[Unit] =
      val _ = (problemSlug, snapshot)
      IO.unit
