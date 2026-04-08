package domains.submission.application

import domains.auth.model.{AuthUser, Username}

object SubmissionPolicy:

  def hasGlobalViewOverride(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canView(actor: AuthUser, submitterUsername: Username): Boolean =
    hasGlobalViewOverride(actor) || actor.username.value == submitterUsername.value
