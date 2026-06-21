package domains.blog.api

import cats.effect.IO
import domains.auth.objects.internal.AuthenticatedUser
import shared.api.{ApiMessages, HttpApiError}

private[api] object ProblemBlogAccess:

  def canManageProblemCatalog(actor: AuthenticatedUser): Boolean =
    actor.problemManager

  def requireProblemCatalogManager(actor: AuthenticatedUser): IO[Unit] =
    HttpApiError.ensure(
      canManageProblemCatalog(actor),
      HttpApiError.forbidden(ApiMessages.problemBlogLinkManageForbidden)
    )
