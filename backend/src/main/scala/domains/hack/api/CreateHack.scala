package domains.hack.api

import cats.effect.IO
import cats.syntax.all.*
import domains.auth.api.AuthenticatedApi
import domains.auth.objects.internal.AuthenticatedUser
import domains.hack.objects.request.CreateHackRequest
import domains.hack.objects.response.HackDetail
import domains.hack.table.hack.{HackMutationTable, HackQueryTable}
import domains.problem.utils.ProblemDataStorage
import domains.submission.objects.SubmissionId
import domains.submission.utils.SubmissionProgramStorage
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.{Method, Request, Status}
import shared.api.{ApiPath, HttpApiError, MultipartTextSupport, PathParams}

import java.sql.Connection
import scala.util.Try

final case class CreateHack(
  submissionProgramStorage: SubmissionProgramStorage,
  problemDataStorage: ProblemDataStorage
) extends AuthenticatedApi[CreateHackRequest, HackDetail]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/hacks")
  override val successStatus: Status = Status.Created
  override protected val outputEncoder: Encoder[HackDetail] = summon[Encoder[HackDetail]]

  override def decode(request: Request[IO], pathParams: PathParams): IO[CreateHackRequest] =
    val _ = pathParams
    if MultipartTextSupport.isMultipart(request) then decodeMultipart(request)
    else request.as[CreateHackRequest]

  override def plan(connection: Connection, actor: AuthenticatedUser, request: CreateHackRequest): IO[HackDetail] =
    val normalizedInput = HackApiSupport.normalizeHackInput(request.input)
    for
      context <- HackApiSupport.loadTargetContext(
        connection,
        actor,
        request.targetSubmissionId,
        request.subtaskIndex,
        submissionProgramStorage,
        problemDataStorage
      )
      _ <- HackApiSupport.validateHackText(normalizedInput, request.strategyProviderSource, HackApiSupport.requiresStrategyProvider(context.subtask))
      hackUuid <- IO.randomUUID
      createdAt <- IO.realTimeInstant
      oldScore = HackApiSupport.targetSubtaskWorstScore(context.submission, context.subtask.index)
      normalizedStrategy = request.strategyProviderSource.filter(_.trim.nonEmpty)
      hackId <- HackMutationTable.insertAttempt(
        connection = connection,
        id = hackUuid,
        problemId = context.submission.problemId,
        targetSubmissionId = context.submission.id,
        authorUsername = actor.username,
        subtaskIndex = context.subtask.index,
        subtaskLabel = context.subtask.label,
        input = normalizedInput,
        strategyProviderSource = normalizedStrategy,
        oldScore = oldScore,
        createdAt = createdAt
      )
      detail <- HackQueryTable.findVisibleById(connection, actor, hackId).flatMap {
        case Some(value) => IO.pure(value)
        case None => HttpApiError.raise(HttpApiError.internal("Hack attempt disappeared after creation."))
      }
    yield detail

  private def decodeMultipart(request: Request[IO]): IO[CreateHackRequest] =
    val scalarMaxBytes = 64L
    val inputMaxBytes = HackApiSupport.MaxHackInputChars.toLong * 4L
    val strategyMaxBytes = HackApiSupport.MaxStrategyProviderChars.toLong * 4L
    for
      multipart <- request.as[Multipart[IO]]
      targetSubmissionIdText <- MultipartTextSupport.requireUtf8Text(multipart, "targetSubmissionId", scalarMaxBytes)
      targetSubmissionId <- HttpApiError.fromEitherBadRequest(SubmissionId.parse(targetSubmissionIdText))
      subtaskIndexText <- MultipartTextSupport.requireUtf8Text(multipart, "subtaskIndex", scalarMaxBytes)
      subtaskIndex <- HttpApiError.fromEitherBadRequest(
        Try(subtaskIndexText.trim.toInt).toEither.left.map(_ => "Subtask index is invalid.")
      )
      inputPart <- requireExactlyOnePart(multipart, "inputText", "inputFile")
      input <- MultipartTextSupport.decodeUtf8Text(inputPart, inputPart.name.getOrElse("input"), inputMaxBytes)
      strategyProviderPart <- optionalExclusivePart(multipart, "strategyProviderSource", "strategyProviderFile")
      strategyProviderSource <- strategyProviderPart.traverse(part =>
        MultipartTextSupport.decodeUtf8Text(part, part.name.getOrElse("strategyProviderSource"), strategyMaxBytes)
      )
    yield CreateHackRequest(targetSubmissionId, subtaskIndex, input, strategyProviderSource)

  private def requireExactlyOnePart(multipart: Multipart[IO], firstField: String, secondField: String): IO[Part[IO]] =
    (MultipartTextSupport.partsNamed(multipart, firstField), MultipartTextSupport.partsNamed(multipart, secondField)) match
      case (first :: Nil, Nil) => IO.pure(first)
      case (Nil, second :: Nil) => IO.pure(second)
      case (Nil, Nil) => HttpApiError.raise(HttpApiError.badRequest(s"Exactly one of '$firstField' or '$secondField' is required."))
      case _ => HttpApiError.raise(HttpApiError.badRequest(s"Exactly one of '$firstField' or '$secondField' must be provided."))

  private def optionalExclusivePart(multipart: Multipart[IO], firstField: String, secondField: String): IO[Option[Part[IO]]] =
    (MultipartTextSupport.partsNamed(multipart, firstField), MultipartTextSupport.partsNamed(multipart, secondField)) match
      case (Nil, Nil) => IO.pure(None)
      case (first :: Nil, Nil) => IO.pure(Some(first))
      case (Nil, second :: Nil) => IO.pure(Some(second))
      case _ => HttpApiError.raise(HttpApiError.badRequest(s"At most one of '$firstField' or '$secondField' may be provided."))
