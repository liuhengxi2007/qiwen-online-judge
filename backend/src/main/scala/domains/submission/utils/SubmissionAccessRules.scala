package domains.submission.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.OtherUserSubmissionAccess
import domains.user.objects.Username

object SubmissionAccessRules:
  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.problemManager

  def canViewOwnOrWithGlobalOverride(actor: AuthenticatedUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value

  def canViewDetailOfOthers(otherUserSubmissionAccess: OtherUserSubmissionAccess): Boolean =
    otherUserSubmissionAccess match
      case OtherUserSubmissionAccess.None => false
      case OtherUserSubmissionAccess.Summary => false
      case OtherUserSubmissionAccess.Detail => true
