package domains.blog.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.blog.application.BlogCommandResults.*
import domains.blog.model.BlogId
import domains.blog.table.BlogTable
import domains.problem.model.ProblemSlug

object BlogQueryCommands:

  def listBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    authorUsername: Option[Username]
  ): IO[ListBlogsResult] =
    databaseSession.withTransactionConnection { connection =>
      authorUsername match
        case Some(username) =>
          BlogTable.listByAuthor(connection, username, actor.username).map(blogs => ListBlogsResult.Listed(blogs))
        case None =>
          BlogTable.listAll(connection, actor.username).map(blogs => ListBlogsResult.Listed(blogs))
    }

  def listProblemBlogs(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    problemSlug: ProblemSlug
  ): IO[ListBlogsResult] =
    databaseSession.withTransactionConnection { connection =>
      BlogTable.listByProblem(connection, problemSlug, actor.username).map(blogs => ListBlogsResult.Listed(blogs))
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
