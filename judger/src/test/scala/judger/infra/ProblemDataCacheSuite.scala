package judger.infra

import cats.effect.IO
import cats.syntax.all.*
import judgeprotocol.objects.{JudgerId, ProblemSlug, SubmissionLanguage}
import judgeprotocol.objects.response.JudgeTaskFileRef
import judger.config.AppConfig
import judger.http.ProblemDataDownloader
import munit.CatsEffectSuite

import java.nio.file.{Files, Path}
import java.security.MessageDigest
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

class ProblemDataCacheSuite extends CatsEffectSuite:

  private val problemSlug = ProblemSlug("sample")

  test("cache hit verifies the existing blob hash") {
    withTempRoot { root =>
      val expectedBytes = "expected".getBytes(java.nio.charset.StandardCharsets.UTF_8)
      val fileRef = refFor("cases/1.in", expectedBytes)
      val blobsRoot = root.resolve("blobs")
      Files.createDirectories(blobsRoot)
      Files.write(blobsRoot.resolve(fileRef.sha256.value), "tampered".getBytes(java.nio.charset.StandardCharsets.UTF_8))

      val cache = ProblemDataCache(config(root), RecordingDownloader(expectedBytes))

      cache.loadBytes(problemSlug, "v1", fileRef).attempt.map { result =>
        assert(result.isLeft)
      }
    }
  }

  test("downloaded blob must match the declared hash") {
    withTempRoot { root =>
      val expectedBytes = "expected".getBytes(java.nio.charset.StandardCharsets.UTF_8)
      val fileRef = refFor("cases/1.in", expectedBytes)
      val cache = ProblemDataCache(config(root), RecordingDownloader("actual".getBytes(java.nio.charset.StandardCharsets.UTF_8)))

      cache.loadBytes(problemSlug, "v1", fileRef).attempt.map { result =>
        assert(result.isLeft)
        assert(!Files.exists(root.resolve("blobs").resolve(fileRef.sha256.value)))
      }
    }
  }

  test("validated sha256 is used as the blob filename") {
    withTempRoot { root =>
      val bytes = "expected".getBytes(java.nio.charset.StandardCharsets.UTF_8)
      val fileRef = refFor("cases/1.in", bytes)
      val downloader = RecordingDownloader(bytes)
      val cache = ProblemDataCache(config(root), downloader)

      cache.loadBytes(problemSlug, "v1", fileRef).map { loaded =>
        assertEquals(loaded.toSeq, bytes.toSeq)
        assertEquals(downloader.requests.toList, List((problemSlug, "cases/1.in")))
        assert(Files.exists(root.resolve("blobs").resolve(fileRef.sha256.value)))
      }
    }
  }

  test("invalid file references are rejected before cache use") {
    assert(JudgeTaskFileRef.from("../secret", 1L, "a" * 64).isLeft)
    assert(JudgeTaskFileRef.from("cases/1.in", 1L, "../outside").isLeft)
  }

  private final class RecordingDownloader(bytes: Array[Byte]) extends ProblemDataDownloader:
    val requests: ListBuffer[(ProblemSlug, String)] = ListBuffer.empty

    override def downloadProblemData(problemSlug: ProblemSlug, path: String): IO[Array[Byte]] =
      IO {
        requests += ((problemSlug, path))
        bytes
      }

  private def refFor(path: String, bytes: Array[Byte]): JudgeTaskFileRef =
    JudgeTaskFileRef.from(path, bytes.length.toLong, sha256Hex(bytes)).fold(message => fail(message), identity)

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def config(root: Path): AppConfig =
    AppConfig(
      backendBaseUrl = "http://localhost:8080",
      judgeToken = "token",
      preferredJudgerPrefix = JudgerId("test"),
      host = "localhost",
      processId = None,
      supportedLanguages = List(SubmissionLanguage.Cpp17),
      pollIntervalMs = 1000L,
      cxx = "g++",
      python3 = "python3",
      isolateBin = "isolate",
      isolateBoxId = 100,
      preferIsolateCgroups = true,
      workRoot = root.resolve("work"),
      problemDataCacheRoot = root
    )

  private def withTempRoot[A](test: Path => IO[A]): IO[A] =
    IO(Files.createTempDirectory("problem-data-cache-suite")).bracket(test) { root =>
      IO.blocking(deleteRecursively(root)).void
    }

  private def deleteRecursively(root: Path): Unit =
    if Files.exists(root) then
      Files.walk(root).iterator().asScala.toList.reverse.foreach(path => Files.deleteIfExists(path))
