package domains.problem.application.output


final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)
