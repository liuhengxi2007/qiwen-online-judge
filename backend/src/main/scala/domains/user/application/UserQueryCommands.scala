package domains.user.application

import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, Username}
import domains.auth.table.AuthUserTable
import domains.blog.table.BlogTable
import domains.shared.model.{PageRequest, PageResponse}
import domains.user.model.{UserAcceptedRanklistItem, UserContribution, UserProfileResponse, UserRanklistItem}

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
      AuthUserTable.findByUsername(connection, targetUsername).flatMap {
        case None =>
          IO.pure(GetUserProfileResult.NotFound)
        case Some(targetUser) =>
          for
            contribution <- BlogTable.contributionByAuthor(connection, targetUsername)
            acceptedProblems <- AuthUserTable.listAcceptedProblems(connection, targetUsername)
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
      AuthUserTable.listContributionRanklist(
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
      AuthUserTable.listAcceptedRanklist(
        connection,
        PageRequest(page = pageRequest.page, pageSize = ranklistPageSize)
      )
    }
