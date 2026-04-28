package domains.problem.application

import domains.problem.model.{ProblemDataFileListResponse, ProblemDataTreeResponse, ProblemDetail}

object ProblemCommandResults:

  enum CreateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case SlugAlreadyExists
    case SlugConflictsWithProblemSet
    case Created(problem: ProblemDetail)

  enum GetProblemResult:
    case NotFound
    case Forbidden
    case Found(problem: ProblemDetail)

  enum UpdateProblemResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: ProblemDetail)

  enum DeleteProblemResult:
    case Forbidden
    case ProblemNotFound
    case Deleted

  enum UpdateProblemDataResult:
    case Forbidden
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Updated(problem: ProblemDetail)

  enum ListProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case Listed(response: ProblemDataFileListResponse)

  enum ListProblemDataTreeResult:
    case Forbidden
    case ProblemNotFound
    case Listed(response: ProblemDataTreeResponse)

  enum AuthorizeProblemDataDownloadResult:
    case Forbidden
    case ProblemNotFound
    case Authorized

  enum DeleteProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case DataFileNotFound
    case Deleted(problem: ProblemDetail)

  enum ClearProblemDataResult:
    case Forbidden
    case ProblemNotFound
    case Cleared(problem: ProblemDetail)
