package domains.blog.table

import domains.auth.table.UserIdentityTableSupport.readUserIdentity
import domains.blog.model.{BlogCommentContent, BlogCommentId, BlogCommentSummary, BlogContent, BlogId, BlogProblemReference, BlogSummary, BlogTitle, BlogVisibility, BlogVote}
import domains.problem.model.{ProblemSlug, ProblemTitle}

import java.sql.ResultSet

object BlogTableSupport:

  def readBlogSummary(resultSet: ResultSet): BlogSummary =
    BlogSummary(
      id = BlogId(resultSet.getLong("public_id")),
      title = parseColumn("blogs.title", resultSet.getString("title"), BlogTitle.parse),
      content = parseColumn("blogs.content", resultSet.getString("content"), BlogContent.parse),
      author = readUserIdentity(resultSet, "author"),
      visibility = parseColumn("blogs.visibility", resultSet.getString("visibility"), BlogVisibility.parse),
      relatedProblems = Nil,
      score = resultSet.getInt("score"),
      viewerVote = Option(resultSet.getString("viewer_vote")).flatMap(BlogVote.fromDatabase),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def readBlogProblemReference(resultSet: ResultSet): BlogProblemReference =
    BlogProblemReference(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse)
    )

  def readBlogCommentSummary(resultSet: ResultSet): BlogCommentSummary =
    BlogCommentSummary(
      id = BlogCommentId(resultSet.getLong("public_id")),
      parentId = readOptionalLong(resultSet, "parent_id").map(BlogCommentId(_)),
      content = parseColumn("blog_comments.content", resultSet.getString("content"), BlogCommentContent.parse),
      author = readUserIdentity(resultSet, "author"),
      score = resultSet.getInt("score"),
      viewerVote = Option(resultSet.getString("viewer_vote")).flatMap(BlogVote.fromDatabase),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  def readOptionalLong(resultSet: ResultSet, columnName: String): Option[Long] =
    val value = resultSet.getLong(columnName)
    if resultSet.wasNull() then None else Some(value)
