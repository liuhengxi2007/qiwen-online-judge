package domains.problem.application

import domains.problem.model.{ProblemDataFilename, ProblemDataPath}
import domains.shared.upload.{FileUploadPolicy, FileUploadPreparation, PreparedUploadFile, StoredFilePath}

object ProblemDataUploadPreparation:

  private val uploadPolicy = FileUploadPolicy.preserve

  def prepareSingleFile(
    path: ProblemDataPath,
    bytes: Array[Byte]
  ): Either[String, PreparedUploadFile] =
    FileUploadPreparation.prepareFile(path.toStoredFilePath, bytes, uploadPolicy)

  def prepareArchive(
    archiveBytes: Array[Byte],
    targetDirectory: Option[ProblemDataPath]
  ): Either[String, List[PreparedUploadFile]] =
    FileUploadPreparation.prepareArchive(archiveBytes, targetDirectory.map(_.toStoredFilePath), uploadPolicy)

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

  def toProblemDataPath(path: StoredFilePath): Either[String, ProblemDataPath] =
    ProblemDataPath.parse(path.value)

  private def flattenPreparedFile(file: PreparedUploadFile): PreparedUploadFile =
    PreparedUploadFile(
      path = StoredFilePath(file.path.fileName),
      bytes = file.bytes
    )
