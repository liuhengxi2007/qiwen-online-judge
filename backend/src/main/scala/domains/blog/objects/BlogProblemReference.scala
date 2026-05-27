package domains.blog.objects



import domains.problem.objects.{ProblemSlug, ProblemTitle}

final case class BlogProblemReference(
  slug: ProblemSlug,
  title: ProblemTitle
)
