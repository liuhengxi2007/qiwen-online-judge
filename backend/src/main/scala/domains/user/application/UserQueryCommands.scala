package domains.user.application



import cats.effect.IO
import database.DatabaseSession
import domains.auth.model.{AuthUser, SiteManagerUser, Username}
import domains.blog.table.BlogTable
import shared.model.{PageRequest, PageResponse}
import domains.user.application.output.{UserAcceptedRanklistItem, UserListResponse, UserProfileResponse, UserRanklistItem}
import domains.user.application.input.UserSearchQuery
import domains.user.model.{UserContribution, UserIdentity}
import domains.user.application.input.{UserListRequest}
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
    databaseSession.withTransactionConnection { connection =>
      UserTable.listUsers(connection, actor, request)
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
    query: UserSearchQuery
  ): IO[List[UserIdentity]] =
    val _ = actor
    if query.value.length < minSuggestionQueryLength then IO.pure(List.empty)
    else
      databaseSession.withTransactionConnection { connection =>
        UserTable.listSuggestions(connection, query)
      }
