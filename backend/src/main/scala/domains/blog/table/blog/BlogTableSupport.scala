package domains.blog.table.blog



import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogContent, BlogId, BlogProblemReference, BlogTitle, BlogVisibility, BlogVote}
import domains.blog.objects.response.{BlogCommentSummary, BlogSummary}
import domains.problem.objects.{ProblemSlug, ProblemTitle}
import database.utils.UserIdentitySql
import domains.user.objects.{DisplayName, UserIdentity, Username}

import java.sql.ResultSet

/** 博客表读写辅助对象，集中处理数据库列和博客领域对象之间的转换。 */
object BlogTableSupport:

  /** 从 ResultSet 读取指定前缀的用户身份。 */
  def readUserIdentity(resultSet: ResultSet, prefix: String): UserIdentity =
    val row = UserIdentitySql.readUserIdentityRow(resultSet, prefix)
    UserIdentity(
      username = Username.canonical(row.username),
      displayName = DisplayName(row.displayName)
    )

  /** 从博客查询行读取博客摘要，不包含关联题目补全。 */
  def readBlogSummary(resultSet: ResultSet): BlogSummary =
    BlogSummary(
      id = BlogId(resultSet.getLong("public_id")),
      title = parseColumn("blogs.title", resultSet.getString("title"), BlogTitle.parse),
      content = parseColumn("blogs.content", resultSet.getString("content"), BlogContent.parse),
      author = readUserIdentity(resultSet, "author"),
      visibility = parseColumn("blogs.visibility", resultSet.getString("visibility"), BlogVisibility.parse),
      relatedProblems = Nil,
      score = resultSet.getInt("score"),
      viewerVote = Option(resultSet.getString("viewer_vote")).flatMap(decodeBlogVoteColumn),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 从关联题目查询行读取博客题目引用。 */
  def readBlogProblemReference(resultSet: ResultSet): BlogProblemReference =
    BlogProblemReference(
      slug = parseColumn("problems.slug", resultSet.getString("slug"), ProblemSlug.parse),
      title = parseColumn("problems.title", resultSet.getString("title"), ProblemTitle.parse)
    )

  /** 从评论查询行读取评论摘要和当前用户投票。 */
  def readBlogCommentSummary(resultSet: ResultSet): BlogCommentSummary =
    BlogCommentSummary(
      id = BlogCommentId(resultSet.getLong("public_id")),
      parentId = readOptionalLong(resultSet, "parent_id").map(BlogCommentId(_)),
      content = parseColumn("blog_comments.content", resultSet.getString("content"), BlogCommentContent.parse),
      author = readUserIdentity(resultSet, "author"),
      score = resultSet.getInt("score"),
      viewerVote = Option(resultSet.getString("viewer_vote")).flatMap(decodeBlogVoteColumn),
      createdAt = resultSet.getTimestamp("created_at").toInstant,
      updatedAt = resultSet.getTimestamp("updated_at").toInstant
    )

  /** 将博客可见性编码为数据库列值。 */
  def encodeBlogVisibilityColumn(visibility: BlogVisibility): String =
    visibility match
      case BlogVisibility.Public => "public"
      case BlogVisibility.Private => "private"

  /** 将投票方向编码为数据库列值。 */
  def encodeBlogVoteColumn(vote: BlogVote): String =
    vote match
      case BlogVote.Up => "up"
      case BlogVote.Down => "down"

  /** 将数据库投票列值解码为投票方向，非法值返回 None。 */
  def decodeBlogVoteColumn(value: String): Option[BlogVote] =
    BlogVote.parse(value).toOption

  /** 注意：按领域解析函数读取必填列；这里抛异常表示数据库已有非法值，不是可恢复的用户输入错误。 */
  def parseColumn[A](columnName: String, rawValue: String, parse: String => Either[String, A]): A =
    parse(rawValue).fold(message => throw IllegalStateException(s"Invalid value in $columnName: $message"), identity)

  /** 注意：INSERT RETURNING 没有返回行时抛出内部数据异常，因为正常数据库语义下该分支不可达。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  /** 从 ResultSet 读取可空 Long 列，并使用 wasNull 区分真实 0 与 null。 */
  def readOptionalLong(resultSet: ResultSet, columnName: String): Option[Long] =
    val value = resultSet.getLong(columnName)
    if resultSet.wasNull() then None else Some(value)
