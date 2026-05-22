package domains.blog.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.AuthUser
import domains.user.model.Username
import domains.blog.application.BlogCommandResults.*
import domains.blog.model.BlogId
import domains.blog.table.BlogTable
import domains.problem.model.ProblemSlug
import shared.model.PageRequest

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
          BlogTable.listByAuthor(connection, username, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
        case None =>
          BlogTable.listAll(connection, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def listProblemBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    pageRequest: PageRequest
  ): IO[ListBlogsResult] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      BlogTable.listByProblem(connection, problemSlug, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def listPendingProblemBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: ProblemSlug,
    pageRequest: PageRequest
  ): IO[ListBlogsResult] =
    val normalizedPageRequest = pageRequest.normalized
    databaseSession.withTransactionConnection { connection =>
      if !domains.problem.application.ProblemPolicy.canEdit(actor) then
        IO.pure(ListBlogsResult.Listed(shared.model.PageResponse(Nil, normalizedPageRequest.page, normalizedPageRequest.pageSize, 0L)))
      else BlogTable.listPendingByProblem(connection, problemSlug, actor.username, normalizedPageRequest).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def getBlog(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    blogId: BlogId
  ): IO[GetBlogResult] =
    databaseSession.withTransactionConnection { connection =>
      BlogTable.findById(connection, blogId, actor.username).map {
        case Some(blog) => GetBlogResult.Found(blog)
        case None => GetBlogResult.NotFound
      }
    }
