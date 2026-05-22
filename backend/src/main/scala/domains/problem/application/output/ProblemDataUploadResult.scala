package domains.problem.application.output

import domains.problem.model.*

final case class ProblemDataUploadResult(
  problem: ProblemDetail,
  uploadedFileCount: Int
)
