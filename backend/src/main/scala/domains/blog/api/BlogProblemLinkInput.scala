package domains.blog.api

import cats.effect.IO
import domains.blog.objects.BlogId
import domains.problem.objects.ProblemSlug
import shared.api.{HttpApiError, PathParams}

private[api] final case class BlogProblemLinkInput(
  problemSlug: ProblemSlug,
  blogId: BlogId
)

private[api] object BlogProblemLinkInput:

  def fromPathParams(pathParams: PathParams): IO[BlogProblemLinkInput] =
    HttpApiError.fromEitherBadRequest {
      for
        problemSlug <- pathParams.require("problemSlug").flatMap(ProblemSlug.parse)
        blogId <- pathParams.require("blogId").flatMap(BlogId.parse)
      yield BlogProblemLinkInput(problemSlug, blogId)
    }
