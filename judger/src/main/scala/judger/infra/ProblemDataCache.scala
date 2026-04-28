package judger.infra

import cats.effect.IO
import judgeprotocol.model.{JudgeTask, JudgeTaskFileRef, ProblemSlug}
import judger.config.AppConfig

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import java.security.MessageDigest

final class ProblemDataCache(config: AppConfig, httpClient: JudgeHttpClient):

  def loadBytes(problemSlug: ProblemSlug, problemDataVersion: String, fileRef: JudgeTaskFileRef): IO[Array[Byte]] =
    for
      _ <- rememberManifest(problemSlug, problemDataVersion, fileRef)
      blobPath <- ensureBlob(problemSlug, fileRef)
      bytes <- IO.blocking(Files.readAllBytes(blobPath))
    yield bytes

  private def ensureBlob(problemSlug: ProblemSlug, fileRef: JudgeTaskFileRef): IO[Path] =
    val targetPath = config.problemDataCacheRoot.resolve("blobs").resolve(fileRef.sha256)
    IO.blocking {
      Files.createDirectories(targetPath.getParent)
      targetPath
    } *> IO.blocking(Files.exists(targetPath)).flatMap {
      case true =>
        IO.pure(targetPath)
      case false =>
        for
          bytes <- httpClient.downloadProblemData(problemSlug, fileRef.path)
          _ <- verifyHash(fileRef, bytes)
          _ <- IO.blocking {
            val tempPath = targetPath.resolveSibling(targetPath.getFileName.toString + ".tmp")
            Files.write(tempPath, bytes)
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING)
          }
        yield targetPath
    }

  private def rememberManifest(problemSlug: ProblemSlug, version: String, fileRef: JudgeTaskFileRef): IO[Unit] =
    val manifestPath = config.problemDataCacheRoot.resolve("manifests").resolve(s"${problemSlug.value}-$version.txt")
    IO.blocking {
      Files.createDirectories(manifestPath.getParent)
      val line = s"${fileRef.path}\t${fileRef.sizeBytes}\t${fileRef.sha256}\n"
      if !Files.exists(manifestPath) then Files.writeString(manifestPath, line, StandardCharsets.UTF_8)
      else Files.writeString(manifestPath, line, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)
    }.void.handleError(_ => ())

  private def verifyHash(fileRef: JudgeTaskFileRef, bytes: Array[Byte]): IO[Unit] =
    val actual = sha256Hex(bytes)
    if actual == fileRef.sha256 then IO.unit
    else IO.raiseError(RuntimeException(s"Cached problem data hash mismatch for ${fileRef.path}."))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
