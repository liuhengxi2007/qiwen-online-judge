package domains.problemset.application

import domains.problem.application.ProblemCommands
import domains.problemset.model.ProblemSet

object ProblemSetDecisions:
  type ProblemSetMemberTarget = ProblemCommands.ProblemSetMemberTarget

  enum CreateProblemSetDecision:
    case SlugAlreadyExists
    case SlugConflictsWithProblem
    case ValidationFailed(message: String)
    case Create

  enum UpdateProblemSetDecision:
    case ProblemSetNotFound
    case ValidationFailed(message: String)
    case Update(problemSet: ProblemSet)

  enum AddProblemDecision:
    case ProblemSetNotFound
    case ProblemNotFound
    case Link(problemSet: ProblemSet, problem: ProblemSetMemberTarget)

  enum RemoveProblemDecision:
    case ProblemSetNotFound
    case ProblemNotFound
    case Remove(problemSet: ProblemSet, problem: ProblemSetMemberTarget)

  def decideCreateProblemSet(
    existingProblemSet: Option[ProblemSet],
    hasConflictingProblemSlug: Boolean,
    accessPolicyValidation: Option[String],
  ): CreateProblemSetDecision =
    existingProblemSet match
      case Some(_) => CreateProblemSetDecision.SlugAlreadyExists
      case None if hasConflictingProblemSlug => CreateProblemSetDecision.SlugConflictsWithProblem
      case None =>
        accessPolicyValidation match
          case Some(message) => CreateProblemSetDecision.ValidationFailed(message)
          case None => CreateProblemSetDecision.Create

  def decideUpdateProblemSet(
    maybeProblemSet: Option[ProblemSet],
    accessPolicyValidation: Option[String],
  ): UpdateProblemSetDecision =
    maybeProblemSet match
      case None => UpdateProblemSetDecision.ProblemSetNotFound
      case Some(_) if accessPolicyValidation.nonEmpty => UpdateProblemSetDecision.ValidationFailed(accessPolicyValidation.get)
      case Some(problemSet) => UpdateProblemSetDecision.Update(problemSet)

  def decideAddProblem(
    maybeProblemSet: Option[ProblemSet],
    maybeProblem: Option[ProblemSetMemberTarget],
  ): AddProblemDecision =
    maybeProblemSet match
      case None => AddProblemDecision.ProblemSetNotFound
      case Some(_) if maybeProblem.isEmpty => AddProblemDecision.ProblemNotFound
      case Some(problemSet) => AddProblemDecision.Link(problemSet, maybeProblem.get)

  def decideRemoveProblem(
    maybeProblemSet: Option[ProblemSet],
    maybeProblem: Option[ProblemSetMemberTarget],
  ): RemoveProblemDecision =
    maybeProblemSet match
      case None => RemoveProblemDecision.ProblemSetNotFound
      case Some(_) if maybeProblem.isEmpty => RemoveProblemDecision.ProblemNotFound
      case Some(problemSet) => RemoveProblemDecision.Remove(problemSet, maybeProblem.get)
