package domains.problem.utils



import domains.problem.objects.{ProblemDataFilename, ProblemDataPath}
import shared.application.upload.{FileUploadPolicy, FileUploadPreparation, PreparedUploadFile, StoredFilePath}

/** 题目数据上传预处理；统一复用共享上传策略来校验路径、解压归档并保留目录结构。 */
object ProblemDataUploadPreparation:

  private val uploadPolicy = FileUploadPolicy.preserve

  /** 校验并封装单文件上传；输出可直接写入对象存储的文件描述。 */
  def prepareSingleFile(
    path: ProblemDataPath,
    bytes: Array[Byte]
  ): Either[String, PreparedUploadFile] =
    FileUploadPreparation.prepareFile(path.toStoredFilePath, bytes, uploadPolicy)

  /** 解压并校验 zip 归档；targetDirectory 存在时会把归档内容放入该目录下。 */
  def prepareArchive(
    archiveBytes: Array[Byte],
    targetDirectory: Option[ProblemDataPath]
  ): Either[String, List[PreparedUploadFile]] =
    FileUploadPreparation.prepareArchive(archiveBytes, targetDirectory.map(_.toStoredFilePath), uploadPolicy)

  /** 兼容旧版按文件名上传；zip 会被解包并扁平化到根目录，其它文件按原文件名写入。 */
  def prepareLegacyUpload(
    filename: ProblemDataFilename,
    bytes: Array[Byte]
  ): Either[String, List[PreparedUploadFile]] =
    if filename.value.toLowerCase.endsWith(".zip") then
      prepareArchive(bytes, None)
        .map(_.map(flattenPreparedFile))
    else
      FileUploadPreparation
        .prepareFile(StoredFilePath(filename.value), bytes, uploadPolicy)
        .map(file => List(file))

  /** 将共享上传路径转换为题目数据路径，保证后续存储仍使用题目域类型。 */
  def toProblemDataPath(path: StoredFilePath): Either[String, ProblemDataPath] =
    ProblemDataPath.parse(path.value)

  private def flattenPreparedFile(file: PreparedUploadFile): PreparedUploadFile =
    PreparedUploadFile(
      path = StoredFilePath(file.path.fileName),
      bytes = file.bytes
    )
