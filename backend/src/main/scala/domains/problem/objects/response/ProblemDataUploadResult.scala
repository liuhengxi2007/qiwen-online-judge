package domains.problem.objects.response


final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)
