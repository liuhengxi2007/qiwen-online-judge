package domains.user.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.blog.table.BlogTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.user.model.{UserAcceptedRanklistItem, UserContribution, UserProfileResponse, UserRanklistItem}
import domains.user.table.UserTable

object UserQueryCommands:

  enum GetUserProfileResult:
    case NotFound
    case Found(profile: UserProfileResponse)

  private val ranklistPageSize = 10

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
