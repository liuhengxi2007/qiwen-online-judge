package domains.problem.model.response


final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)
