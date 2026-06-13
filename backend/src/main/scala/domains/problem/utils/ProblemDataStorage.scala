package domains.problem.utils



import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.{ProblemDataFilename, ProblemDataPath, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifest

/** 题目数据对象存储抽象；路径均为题目 slug 下的相对路径，所有方法都有外部存储副作用或读取。 */
trait ProblemDataStorage:
  import ProblemDataStorage.ProblemDataSnapshot

  /** 列出题目下所有对象路径；输出按实现约定排序，不包含目录占位。 */
  def listPaths(problemSlug: ProblemSlug): IO[List[ProblemDataPath]]
  /** 读取当前对象内容并生成判题数据清单；会扫描并计算每个文件哈希。 */
  def describeManifest(problemSlug: ProblemSlug): IO[ProblemDataManifest]
  /** 为回滚准备完整目录快照；可能读取大量对象内容。 */
  def snapshotDirectory(problemSlug: ProblemSlug): IO[ProblemDataSnapshot]
  /** 写入或覆盖单个题目数据对象，返回实际写入路径。 */
  def writePath(problemSlug: ProblemSlug, path: ProblemDataPath, bytes: Array[Byte]): IO[ProblemDataPath]
  /** 读取单个题目数据对象；不存在时返回 None。 */
  def readPath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Option[(ProblemDataPath, Array[Byte])]]
  /** 删除单个题目数据对象；返回对象是否曾存在。 */
  def deletePath(problemSlug: ProblemSlug, path: ProblemDataPath): IO[Boolean]
  /** 删除题目下所有数据对象。 */
  def deleteAllFiles(problemSlug: ProblemSlug): IO[Unit]
  /** 用快照替换题目目录内容，通常用于上传/删除失败后的补偿恢复。 */
  def restoreDirectory(problemSlug: ProblemSlug, snapshot: ProblemDataSnapshot): IO[Unit]

/** 题目数据存储的兼容扩展方法；保留旧版按根文件名操作的接口。 */
object ProblemDataStorage:

  type ProblemDataSnapshot = Map[ProblemDataPath, Array[Byte]]

  extension (storage: ProblemDataStorage)

    /** 将路径列表转换为根文件名列表；遇到无法表示为文件名的路径会失败。 */
    def listFiles(problemSlug: ProblemSlug): IO[List[ProblemDataFilename]] =
      storage.listPaths(problemSlug).flatMap { paths =>
        IO.fromEither(
          paths
            .traverse(path => ProblemDataFilename.parse(path.fileName))
            .left
            .map(message => IllegalArgumentException(message))
        )
      }

    /** 以根文件名写入对象，用于旧接口兼容。 */
    def writeFile(problemSlug: ProblemSlug, filename: ProblemDataFilename, bytes: Array[Byte]): IO[ProblemDataFilename] =
      storage.writePath(problemSlug, ProblemDataPath.fromFilename(filename), bytes)
        .flatMap(path =>
          IO.fromEither(
            ProblemDataFilename
              .parse(path.fileName)
              .left
              .map(message => IllegalArgumentException(message))
          )
        )

    /** 以根文件名读取对象，用于旧接口兼容。 */
    def readFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Option[(ProblemDataFilename, Array[Byte])]] =
      storage.readPath(problemSlug, ProblemDataPath.fromFilename(filename)).flatMap {
        case None => IO.pure(None)
        case Some((path, bytes)) =>
          IO.fromEither(
            ProblemDataFilename
              .parse(path.fileName)
              .left
              .map(message => IllegalArgumentException(message))
          ).map(parsed => Some((parsed, bytes)))
      }

    /** 以根文件名删除对象，用于旧接口兼容。 */
    def deleteFile(problemSlug: ProblemSlug, filename: ProblemDataFilename): IO[Boolean] =
      storage.deletePath(problemSlug, ProblemDataPath.fromFilename(filename))
