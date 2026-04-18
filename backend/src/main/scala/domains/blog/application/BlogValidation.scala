package domains.blog.application

import domains.blog.model.{BlogType, CreateBlogRequest, UpdateBlogRequest}
import domains.problem.model.ProblemSlug

object BlogValidation:

  def validateCreate(request: CreateBlogRequest): Either[String, CreateBlogRequest] =
    for
      title <- request.title.value match
        case value if value.trim.nonEmpty => Right(request.title)
        case _ => Left("Blog title is required.")
      content <- request.content.value match
        case value if value.trim.nonEmpty => Right(request.content)
        case _ => Left("Blog content is required.")
      problemSlug <- validateProblemLink(request.blogType, request.problemSlug)
    yield CreateBlogRequest(title, content, request.visibility, request.blogType, problemSlug)

  def validateUpdate(request: UpdateBlogRequest): Either[String, UpdateBlogRequest] =
    for
      title <- request.title.value match
        case value if value.trim.nonEmpty => Right(request.title)
        case _ => Left("Blog title is required.")
      content <- request.content.value match
        case value if value.trim.nonEmpty => Right(request.content)
        case _ => Left("Blog content is required.")
      problemSlug <- validateProblemLink(request.blogType, request.problemSlug)
    yield UpdateBlogRequest(title, content, request.visibility, request.blogType, problemSlug)

  def validateProblemLink(blogType: BlogType, problemSlug: Option[ProblemSlug]): Either[String, Option[ProblemSlug]] =
    blogType match
      case BlogType.General => Right(None)
      case BlogType.Problem =>
        problemSlug match
          case Some(slug) => Right(Some(slug))
          case None => Left("Problem blog requires a linked problem.")
