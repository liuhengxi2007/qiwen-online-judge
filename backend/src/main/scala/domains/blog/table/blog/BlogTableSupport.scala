package domains.blog.table.blog



import domains.blog.objects.{BlogCommentContent, BlogCommentId, BlogContent, BlogId, BlogProblemReference, BlogTitle, BlogVote}
import domains.blog.objects.response.{BlogCommentSummary, BlogSummary}
import domains.problem.objects.{ProblemSlug, ProblemTitle}
import database.utils.UserIdentitySql
import domains.user.objects.{DisplayName, UserIdentity, Username}
import shared.objects.access.{BaseAccess, ResourceVisibilityPolicy}

import java.sql.{PreparedStatement, ResultSet}

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
      visibilityPolicy = ResourceVisibilityPolicy(
        parseOptionalColumn("blogs.base_access", resultSet.getString("base_access"), decodeBaseAccessColumn),
        Nil
      ),
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

  /** 将基础可见性编码为数据库列值。 */
  def encodeBaseAccessColumn(baseAccess: BaseAccess): String =
    baseAccess match
      case BaseAccess.Restricted => "restricted"
      case BaseAccess.Public => "public"

  /** 将数据库基础可见性列值解码为领域枚举。 */
  def decodeBaseAccessColumn(value: String): Option[BaseAccess] =
    BaseAccess.parse(value).toOption

  /** 生成博客对某个 viewer 可见的 SQL 谓词；调用方需按 bindBlogVisibleToViewer 的顺序绑定 3 个用户名参数。 */
  def blogVisibleToViewerPredicate(blogAlias: String): String =
    s"""
      |(
      |  $blogAlias.author_username = ?
      |  or $blogAlias.base_access = 'public'
      |  or exists (
      |    select 1
      |    from blog_access_grants bag
      |    where bag.blog_id = $blogAlias.id
      |      and bag.subject_kind = 'user'
      |      and bag.subject_key = ?
      |  )
      |  or exists (
      |    select 1
      |    from blog_access_grants bag
      |    join user_groups ug on ug.slug = bag.subject_key
      |    join user_group_memberships ugm on ugm.user_group_id = ug.id
      |    where bag.blog_id = $blogAlias.id
      |      and bag.subject_kind = 'user_group'
      |      and ugm.username = ?
      |  )
      |)
      |""".stripMargin

  /** 绑定博客可见性谓词的用户名参数，并返回下一个参数序号。 */
  def bindBlogVisibleToViewer(statement: PreparedStatement, startIndex: Int, viewerUsername: Username): Int =
    statement.setString(startIndex, viewerUsername.value)
    statement.setString(startIndex + 1, viewerUsername.value)
    statement.setString(startIndex + 2, viewerUsername.value)
    startIndex + 3

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

  /** 注意：按 Option 解码必填枚举列；解析失败表示数据库状态异常。 */
  def parseOptionalColumn[A](columnName: String, rawValue: String, parse: String => Option[A]): A =
    parse(rawValue).getOrElse(throw IllegalStateException(s"Invalid value in $columnName: $rawValue"))

  /** 注意：INSERT RETURNING 没有返回行时抛出内部数据异常，因为正常数据库语义下该分支不可达。 */
  def missingInsertResult(entityName: String): Nothing =
    throw new IllegalStateException(s"Insert succeeded but returned no $entityName")

  /** 从 ResultSet 读取可空 Long 列，并使用 wasNull 区分真实 0 与 null。 */
  def readOptionalLong(resultSet: ResultSet, columnName: String): Option[Long] =
    val value = resultSet.getLong(columnName)
    if resultSet.wasNull() then None else Some(value)
