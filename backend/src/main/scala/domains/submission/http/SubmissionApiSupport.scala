package domains.submission.http

import domains.auth.objects.AuthUser
import domains.problem.objects.OthersSubmissionAccess
import domains.submission.objects.request.*
import shared.http.utils.PageRequestQuerySupport

object SubmissionApiSupport:

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canViewOwnOrWithGlobalOverride(actor: AuthUser, submitterUsername: domains.user.objects.Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value

  def canViewDetailOfOthers(othersSubmissionAccess: OthersSubmissionAccess): Boolean =
    othersSubmissionAccess match
      case OthersSubmissionAccess.None => false
      case OthersSubmissionAccess.Summary => false
      case OthersSubmissionAccess.Detail => true

  def listSubmissionsRequest(queryParams: Map[String, String]): SubmissionListRequest =
    val sort = queryParams
      .get("sort")
      .flatMap(rawSort => SubmissionSort.parse(rawSort).toOption)
      .getOrElse(SubmissionSort.Submitted)
    val direction = queryParams
      .get("direction")
      .flatMap(rawDirection => SubmissionSortDirection.parse(rawDirection).toOption)
      .getOrElse(SubmissionSort.defaultDirection(sort))
    val verdict = queryParams
      .get("verdict")
      .flatMap(rawVerdict => SubmissionVerdictFilter.parse(rawVerdict).toOption)
      .getOrElse(SubmissionVerdictFilter.All)

    SubmissionListRequest(
      userQuery = queryParams
        .get("username")
        .flatMap(rawQuery => SubmissionUserQuery.parse(rawQuery).toOption),
      problemQuery = queryParams
        .get("problem")
        .flatMap(rawQuery => SubmissionProblemQuery.parse(rawQuery).toOption),
      verdict = verdict,
      sort = sort,
      direction = direction,
      pageRequest = PageRequestQuerySupport.parsePageRequest(queryParams)
    )
