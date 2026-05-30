package domains.submission.utils

import domains.auth.objects.internal.AuthenticatedUser
import domains.problem.objects.OthersSubmissionAccess
import domains.user.objects.Username

object SubmissionAccessRules:
  def hasGlobalViewOverride(actor: AuthenticatedUser): Boolean =
    actor.siteManager || actor.problemManager

  def canViewOwnOrWithGlobalOverride(actor: AuthenticatedUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value

  def canViewDetailOfOthers(othersSubmissionAccess: OthersSubmissionAccess): Boolean =
    othersSubmissionAccess match
      case OthersSubmissionAccess.None => false
      case OthersSubmissionAccess.Summary => false
      case OthersSubmissionAccess.Detail => true
