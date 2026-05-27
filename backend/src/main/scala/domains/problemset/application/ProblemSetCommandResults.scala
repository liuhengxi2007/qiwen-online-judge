package domains.problemset.application



import domains.problemset.objects.ProblemSet

object ProblemSetCommandResults:

  enum CreateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithProblem
    case Created(problemSet: ProblemSet)

  enum AddProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemAlreadyLinked
    case Linked(problemSet: ProblemSet)

  enum GetProblemSetResult:
    case NotFound
    case Forbidden
    case Found(problemSet: ProblemSet)

  enum UpdateProblemSetResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemSetNotFound
    case Updated(problemSet: ProblemSet)

  enum DeleteProblemSetResult:
    case Forbidden
    case ProblemSetNotFound
    case Deleted

  enum RemoveProblemResult:
    case Forbidden
    case ProblemSetNotFound
    case ProblemNotFound
    case ProblemNotLinked
    case Removed(problemSet: ProblemSet)
