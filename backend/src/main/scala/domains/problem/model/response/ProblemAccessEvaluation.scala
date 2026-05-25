package domains.problem.model.response

import domains.problem.model.OthersSubmissionAccess

final case class ProblemAccessEvaluation(
  canView: Boolean,
  canManage: Boolean,
  othersSubmissionAccess: OthersSubmissionAccess
)
