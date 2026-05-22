package domains.blog.model



import domains.problem.model.{ProblemSlug, ProblemTitle}

final case class BlogProblemReference(
  slug: ProblemSlug,
  title: ProblemTitle
)
