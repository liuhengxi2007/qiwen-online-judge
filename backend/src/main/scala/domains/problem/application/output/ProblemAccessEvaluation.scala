package domains.problem.application.output

import domains.problem.model.OthersSubmissionAccess

final case class ProblemAccessEvaluation(
  canView: Boolean,
  canManage: Boolean,
  othersSubmissionAccess: OthersSubmissionAccess
)
