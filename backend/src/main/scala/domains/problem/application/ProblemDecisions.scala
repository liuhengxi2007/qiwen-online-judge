package domains.problem.application



import domains.problem.http.response.ProblemDetail

object ProblemDecisions:

  enum CreateProblemDecision:
    case SlugAlreadyExists
    case SlugConflictsWithProblemSet
    case ValidationFailed(message: String)
    case Create

  enum UpdateProblemDecision:
    case ProblemNotFound
    case ValidationFailed(message: String)
    case Update(problem: ProblemDetail)

  enum ProblemDataUpdateDecision:
    case ProblemNotFound
    case Update(problem: ProblemDetail)

  enum ProblemDataDeletionDecision:
    case ProblemNotFound
    case Delete(problem: ProblemDetail)

  enum ProblemDataClearDecision:
    case ProblemNotFound
    case Clear(problem: ProblemDetail)

  def decideCreateProblem(
    existingProblem: Option[ProblemDetail],
    conflictingProblemSet: Option[domains.problemset.model.ProblemSet],
    accessPolicyValidation: Option[String],
  ): CreateProblemDecision =
    existingProblem match
      case Some(_) => CreateProblemDecision.SlugAlreadyExists
      case None if conflictingProblemSet.nonEmpty => CreateProblemDecision.SlugConflictsWithProblemSet
      case None =>
        accessPolicyValidation match
          case Some(message) => CreateProblemDecision.ValidationFailed(message)
          case None => CreateProblemDecision.Create

  def decideUpdateProblem(
    maybeProblem: Option[ProblemDetail],
    accessPolicyValidation: Option[String],
  ): UpdateProblemDecision =
    maybeProblem match
      case None => UpdateProblemDecision.ProblemNotFound
      case Some(_) if accessPolicyValidation.nonEmpty => UpdateProblemDecision.ValidationFailed(accessPolicyValidation.get)
      case Some(problem) => UpdateProblemDecision.Update(problem)

  def decideUpdateProblemData(maybeProblem: Option[ProblemDetail]): ProblemDataUpdateDecision =
    maybeProblem match
      case None => ProblemDataUpdateDecision.ProblemNotFound
      case Some(problem) => ProblemDataUpdateDecision.Update(problem)

  def decideDeleteProblemData(maybeProblem: Option[ProblemDetail]): ProblemDataDeletionDecision =
    maybeProblem match
      case None => ProblemDataDeletionDecision.ProblemNotFound
      case Some(problem) => ProblemDataDeletionDecision.Delete(problem)

  def decideClearProblemData(maybeProblem: Option[ProblemDetail]): ProblemDataClearDecision =
    maybeProblem match
      case None => ProblemDataClearDecision.ProblemNotFound
      case Some(problem) => ProblemDataClearDecision.Clear(problem)
