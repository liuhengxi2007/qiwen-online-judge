package domains.problemset.http.api

import cats.effect.IO
import domains.auth.http.AuthenticatedApi
import domains.auth.objects.AuthUser
import domains.problemset.http.ProblemSetApiSupport
import domains.problemset.http.codec.ProblemSetHttpCodecs.given
import domains.problemset.objects.*
import domains.problemset.objects.request.CreateProblemSetRequest
import domains.problemset.objects.response.ProblemSetDetail
import domains.problemset.rules.ProblemSetAccessRules
import domains.problemset.table.problem_set.ProblemSetTable
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.{Method, Request, Status}
import shared.http.{ApiMessages, ApiPath, HttpApiError, PathParams}

import java.sql.Connection

object CreateProblemSet extends AuthenticatedApi[CreateProblemSetRequest, ProblemSetDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/problem-sets")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[ProblemSetDetail] = summon[Encoder[ProblemSetDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateProblemSetRequest] =
    val _ = pathParams
    request.as[CreateProblemSetRequest]

  override def plan(
    connection: Connection,
    actor: AuthUser,
    request: CreateProblemSetRequest
  ): IO[ProblemSetDetail] =
    for
      _ <- HttpApiError.ensure(
        ProblemSetAccessRules.canManageProblemSets(actor),
        HttpApiError.forbidden(ApiMessages.problemManagerRequired)
      )
      slug <- HttpApiError.fromEitherBadRequest(ProblemSetSlug.parse(request.slug.value))
      title <- HttpApiError.fromEitherBadRequest(ProblemSetTitle.parse(request.title.value))
      description <- HttpApiError.fromEitherBadRequest(ProblemSetDescription.parse(request.description.value))
      validRequest = request.copy(slug = slug, title = title, description = description)
      existing <- ProblemSetTable.findBySlug(connection, validRequest.slug)
      _ <- HttpApiError.ensure(existing.isEmpty, HttpApiError.conflict(ApiMessages.problemSetSlugExists))
      conflictingProblem <- ProblemSetApiSupport.problemSlugExists(connection, validRequest.slug.value)
      _ <- HttpApiError.ensure(!conflictingProblem, HttpApiError.conflict(ApiMessages.problemSetSlugConflictsWithProblem))
      _ <- ProblemSetApiSupport.validateAccessPolicySubjects(connection, validRequest.accessPolicy)
      problemSet <- ProblemSetTable.insert(connection, actor.username, ProblemSetApiSupport.sanitizePolicy(validRequest))
    yield ProblemSetApiSupport.toProblemSetDetail(problemSet)
