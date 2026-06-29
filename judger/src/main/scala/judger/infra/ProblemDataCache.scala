package judger.infra

import cats.effect.IO
import judgeprotocol.objects.ProblemSlug
import judgeprotocol.objects.response.JudgeTaskFileRef
import judger.config.AppConfig
import judger.http.ProblemDataDownloader

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import java.security.MessageDigest

/** 题目数据本地缓存，按 sha256 存储 blob，并通过 backend 下载缺失文件。 */
final class ProblemDataCache(config: AppConfig, httpClient: ProblemDataDownloader):

  /** 读取题目数据字节；会 best-effort 记录 manifest、确保 blob 存在并校验 sha256。 */
  def loadBytes(problemSlug: ProblemSlug, problemDataVersion: String, fileRef: JudgeTaskFileRef): IO[Array[Byte]] =
    for
      _ <- rememberManifest(problemSlug, problemDataVersion, fileRef)
      blobPath <- ensureBlob(problemSlug, fileRef)
      bytes <- IO.blocking(Files.readAllBytes(blobPath))
    yield bytes

  private def ensureBlob(problemSlug: ProblemSlug, fileRef: JudgeTaskFileRef): IO[Path] =
    val targetPath = config.problemDataCacheRoot.resolve("blobs").resolve(fileRef.sha256.value).normalize()
    val blobsRoot = config.problemDataCacheRoot.resolve("blobs").normalize()
    IO.blocking {
      Files.createDirectories(blobsRoot)
      if !targetPath.startsWith(blobsRoot) then
        throw RuntimeException(s"Invalid problem data blob path for ${fileRef.sha256.value}.")
      targetPath
    } *> IO.blocking(Files.exists(targetPath)).flatMap {
      case true =>
        IO.blocking(Files.readAllBytes(targetPath)).flatMap(bytes => verifyHash(fileRef, bytes).as(targetPath))
      case false =>
        for
          bytes <- httpClient.downloadProblemData(problemSlug, fileRef.path.value)
          _ <- verifyHash(fileRef, bytes)
          _ <- IO.blocking {
            val tempPath = Files.createTempFile(blobsRoot, s"${fileRef.sha256.value}.", ".tmp")
            try
              Files.write(tempPath, bytes)
              Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            finally Files.deleteIfExists(tempPath)
          }
        yield targetPath
    }

  private def rememberManifest(problemSlug: ProblemSlug, version: String, fileRef: JudgeTaskFileRef): IO[Unit] =
    val manifestsRoot = config.problemDataCacheRoot.resolve("manifests").normalize()
    val manifestName = sha256Hex(s"${problemSlug.value}\n$version".getBytes(StandardCharsets.UTF_8)) + ".txt"
    val manifestPath = manifestsRoot.resolve(manifestName).normalize()
    IO.blocking {
      Files.createDirectories(manifestsRoot)
      if !manifestPath.startsWith(manifestsRoot) then
        throw RuntimeException("Invalid problem data manifest cache path.")
      val line = s"${fileRef.path.value}\t${fileRef.sizeBytes.value}\t${fileRef.sha256.value}\n"
      if !Files.exists(manifestPath) then Files.writeString(manifestPath, line, StandardCharsets.UTF_8)
      else Files.writeString(manifestPath, line, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)
    }.void.handleError(_ => ())

  private def verifyHash(fileRef: JudgeTaskFileRef, bytes: Array[Byte]): IO[Unit] =
    val actual = sha256Hex(bytes)
    if actual == fileRef.sha256.value then IO.unit
    else IO.raiseError(RuntimeException(s"Cached problem data hash mismatch for ${fileRef.path.value}."))

  private def sha256Hex(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
