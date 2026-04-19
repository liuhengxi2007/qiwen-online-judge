package domains.submission.application

import domains.submission.model.{SubmissionDetail, SubmissionSummary}

object SubmissionCommandResults:

  enum CreateSubmissionResult:
    case ValidationFailed(message: String)
    case ProblemNotFound
    case Forbidden
    case Created(submission: SubmissionDetail)

  enum ListSubmissionsResult:
    case Listed(submissions: List[SubmissionSummary])

  enum GetSubmissionResult:
    case NotFound
    case Forbidden
    case Found(submission: SubmissionDetail)

  enum DeleteSubmissionResult:
    case NotFound
    case Forbidden
    case Deleted

  enum RejudgeSubmissionResult:
    case NotFound
    case Forbidden
    case ValidationFailed(message: String)
    case Rejudged(submission: SubmissionDetail)
