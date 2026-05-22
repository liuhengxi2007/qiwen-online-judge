package domains.submission.application



import domains.auth.model.{AuthUser, Username}
import domains.problem.model.OthersSubmissionAccess

object SubmissionPolicy:

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canViewOwn(actor: AuthUser, submitterUsername: Username): Boolean =
    actor.username.value == submitterUsername.value

  def canViewSummaryOfOthers(othersSubmissionAccess: OthersSubmissionAccess): Boolean =
    othersSubmissionAccess match
      case OthersSubmissionAccess.None => false
      case OthersSubmissionAccess.Summary => true
      case OthersSubmissionAccess.Detail => true

  def canViewDetailOfOthers(othersSubmissionAccess: OthersSubmissionAccess): Boolean =
    othersSubmissionAccess match
      case OthersSubmissionAccess.None => false
      case OthersSubmissionAccess.Summary => false
      case OthersSubmissionAccess.Detail => true

  def canViewOwnOrWithGlobalOverride(actor: AuthUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value
