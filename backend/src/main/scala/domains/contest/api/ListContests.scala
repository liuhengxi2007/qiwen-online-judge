package domains.contest.api

import cats.effect.IO
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.contest.objects.Contest
import domains.contest.objects.response.ContestSummary
import domains.contest.table.contest.ContestTable
import domains.contest.utils.ContestAccessRules
import domains.usergroup.api.ListUserGroupSlugsForMember
import io.circe.Encoder
import org.http4s.{Method, Request, Status}
import shared.api.utils.PageRequestQuerySupport
import shared.api.{ApiPath, PathParams}
import shared.objects.{PageRequest, PageResponse}

import java.sql.Connection
import java.time.Instant

object ListContests extends AuthenticatedApi[PageRequest, PageResponse[ContestSummary]]:

  override val method: Method = Method.GET
  override val path: ApiPath = ApiPath("/api/contests")
  override val successStatus: Status = Status.Ok
  override protected val outputEncoder: Encoder[PageResponse[ContestSummary]] = summon[Encoder[PageResponse[ContestSummary]]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[PageRequest] =
    val _ = pathParams
    IO.pure(PageRequestQuerySupport.parsePageRequest(request.uri.query.params))

  override def plan(
    connection: Connection,
    actor: AuthenticatedUser,
    pageRequest: PageRequest
  ): IO[PageResponse[ContestSummary]] =
    val normalizedPageRequest = pageRequest.normalized
    for
      actorGroupSlugs <- ListUserGroupSlugsForMember.plan(connection, actor.username)
      contests <- ContestTable.listVisibleTo(connection, actor, normalizedPageRequest.page, normalizedPageRequest.pageSize)
      now = Instant.now()
      actorGroupSlugSet = actorGroupSlugs.slugs.toSet
    yield contests.copy(
      items = contests.items.map { contest =>
        contest.copy(
          canViewDetail = ContestAccessRules.canViewContestDetail(
            actor,
            contestSummaryToContest(contest),
            actorGroupSlugSet,
            contest.registrationStatus.isRegistered,
            now
          )
        )
      }
    )

  private def contestSummaryToContest(summary: ContestSummary): Contest =
    Contest(
      id = summary.id,
      slug = summary.slug,
      title = summary.title,
      description = summary.description,
      startAt = summary.startAt,
      endAt = summary.endAt,
      problems = Nil,
      accessPolicy = summary.accessPolicy,
      author = summary.author,
      createdAt = summary.createdAt,
      updatedAt = summary.updatedAt
    )
