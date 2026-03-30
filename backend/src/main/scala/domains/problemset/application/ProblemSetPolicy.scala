package domains.problemset.application

import domains.auth.model.AuthUser

object ProblemSetPolicy:

  def canCreate(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canManageProblems(actor: AuthUser): Boolean =
    actor.siteManager || actor.problemManager

  def canList(actor: AuthUser): Boolean =
    val _ = actor
    true
