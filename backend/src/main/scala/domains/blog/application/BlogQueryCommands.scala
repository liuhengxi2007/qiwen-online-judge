package domains.blog.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.objects.AuthUser
import domains.user.objects.Username
import domains.blog.application.BlogCommandResults.*
import domains.blog.objects.BlogId
import domains.blog.table.blog.{BlogPostQueryTable, BlogProblemLinkQueryTable}
import domains.problem.application.ProblemCommands
import domains.problem.objects.ProblemSlug
import shared.objects.PageRequest

object BlogQueryCommands:

  def listBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    authorUsername: Option[Username],
    pageRequest: PageRequest
  ): IO[ListBlogsResult] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      authorUsername match
        case Some(username) =>
          BlogPostQueryTable.listByAuthor(connection, username, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
        case None =>
          BlogPostQueryTable.listAll(connection, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def listProblemBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    pageRequest: PageRequest
  ): IO[ListBlogsResult] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      BlogProblemLinkQueryTable.listByProblem(connection, problemSlug, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def listPendingProblemBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    pageRequest: PageRequest
  ): IO[ListBlogsResult] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      if !ProblemCommands.canManageProblemCatalog(actor) then
        IO.pure(ListBlogsResult.Listed(shared.objects.PageResponse(Nil, normalizedPageRequest.page, normalizedPageRequest.pageSize, 0L)))
      else BlogProblemLinkQueryTable.listPendingByProblem(connection, problemSlug, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def getBlog(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    blogId: BlogId
  ): IO[GetBlogResult] =
    databaseSession.withTransactionConnection { connection =>
      BlogPostQueryTable.findById(connection, blogId, actor.username).map {
        case Some(blog) => GetBlogResult.Found(blog)
        case None => GetBlogResult.NotFound
      }
    }
