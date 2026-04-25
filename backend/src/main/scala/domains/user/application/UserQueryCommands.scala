package domains.user.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, SiteManagerUser, Username}
import domains.blog.table.BlogTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.user.model.{UserAcceptedRanklistItem, UserContribution, UserIdentity, UserListRequest, UserListResponse, UserProfileResponse, UserRanklistItem}
import domains.user.table.UserTable

object UserQueryCommands:

  enum GetUserProfileResult:
    case NotFound
    case Found(profile: UserProfileResponse)

  private val ranklistPageSize = 10
  private val minSuggestionQueryLength = 1

  def listUsers(
    databaseSession: DatabaseSession,
    actor: SiteManagerUser,
    request: UserListRequest
  ): IO[UserListResponse] =
    val normalizedRequest = request.copy(query = normalizeSearchQuery(request.query))
    databaseSession.withTransactionConnection { connection =>
      UserTable.listUsers(connection, actor, normalizedRequest)
    }

  def getUserProfile(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    targetUsername: Username
  ): IO[GetUserProfileResult] =
    val _ = actor
    databaseSession.withTransactionConnection { connection =>
      UserTable.findByUsername(connection, targetUsername).flatMap {
        case None =>
          IO.pure(GetUserProfileResult.NotFound)
        case Some(targetUser) =>
          for
            contribution <- BlogTable.contributionByAuthor(connection, targetUsername)
            acceptedProblems <- UserTable.listAcceptedProblems(connection, targetUsername)
          yield
            GetUserProfileResult.Found(
              UserProfileResponse(
                username = targetUser.username,
                displayName = targetUser.displayName,
                contribution = UserContribution(contribution),
                acceptedProblems = acceptedProblems
              )
            )
      }
    }

  def listContributionRanklist(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[UserRanklistItem]] =
    val _ = actor
    databaseSession.withTransactionConnection { connection =>
      UserTable.listContributionRanklist(
        connection,
        PageRequest(page = pageRequest.page, pageSize = ranklistPageSize)
      )
    }

  def listAcceptedRanklist(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    pageRequest: PageRequest
  ): IO[PageResponse[UserAcceptedRanklistItem]] =
    val _ = actor
    databaseSession.withTransactionConnection { connection =>
      UserTable.listAcceptedRanklist(
        connection,
        PageRequest(page = pageRequest.page, pageSize = ranklistPageSize)
      )
    }

  def listSuggestions(
    databaseSession: DatabaseSession,
    actor: AuthUser,
    query: String
  ): IO[List[UserIdentity]] =
    val _ = actor
    normalizeSearchQuery(Some(query)) match
      case None => IO.pure(List.empty)
      case Some(trimmedQuery) if trimmedQuery.length < minSuggestionQueryLength => IO.pure(List.empty)
      case Some(trimmedQuery) =>
        databaseSession.withTransactionConnection { connection =>
          UserTable.listSuggestions(connection, trimmedQuery)
        }

  private def normalizeSearchQuery(rawQuery: Option[String]): Option[String] =
    rawQuery.map(_.trim).filter(_.nonEmpty)
