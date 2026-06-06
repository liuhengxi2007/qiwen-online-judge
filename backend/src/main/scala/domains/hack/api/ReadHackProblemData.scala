package domains.hack.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.hack.table.hack.HackJudgeTable
import domains.problem.objects.ProblemSlug
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class ReadHackProblemDataInput(
  problemSlug: ProblemSlug,
  path: String
)

object ReadHackProblemData extends InternalOnlyApi[ReadHackProblemDataInput, Option[(String, Array[Byte])]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/problem-data")

  def input(problemSlug: ProblemSlug, path: String): ReadHackProblemDataInput =
    ReadHackProblemDataInput(problemSlug = problemSlug, path = path)

  override def plan(connection: Connection, input: ReadHackProblemDataInput): IO[Option[(String, Array[Byte])]] =
    HackJudgeTable.readHackDataByPath(connection, input.problemSlug, input.path)
