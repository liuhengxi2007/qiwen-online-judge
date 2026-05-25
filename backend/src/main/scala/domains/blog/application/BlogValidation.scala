package domains.blog.application



import domains.blog.model.request.{CreateBlogRequest, UpdateBlogRequest}

object BlogValidation:

  def validateCreate(request: CreateBlogRequest): Either[String, CreateBlogRequest] =
    for
      title <- request.title.value match
        case value if value.trim.nonEmpty => Right(request.title)
        case _ => Left("Blog title is required.")
      content <- request.content.value match
        case value if value.trim.nonEmpty => Right(request.content)
        case _ => Left("Blog content is required.")
    yield CreateBlogRequest(title, content, request.visibility)

  def validateUpdate(request: UpdateBlogRequest): Either[String, UpdateBlogRequest] =
    for
      title <- request.title.value match
        case value if value.trim.nonEmpty => Right(request.title)
        case _ => Left("Blog title is required.")
      content <- request.content.value match
        case value if value.trim.nonEmpty => Right(request.content)
        case _ => Left("Blog content is required.")
    yield UpdateBlogRequest(title, content, request.visibility)
