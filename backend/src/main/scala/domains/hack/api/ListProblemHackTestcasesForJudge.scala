package domains.hack.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.hack.objects.HackId
import domains.hack.table.hack.HackJudgeTable
import domains.problem.objects.ProblemId
import judgeprotocol.objects.response.JudgeTaskFileRef
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

final case class ProblemHackTestcaseForJudge(
  hackId: HackId,
  subtaskIndex: Int,
  inputRef: JudgeTaskFileRef,
  answerRef: Option[JudgeTaskFileRef]
)

object ListProblemHackTestcasesForJudge extends InternalOnlyApi[ProblemId, List[ProblemHackTestcaseForJudge]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/hacks/judge/testcases")

  override def plan(connection: Connection, problemId: ProblemId): IO[List[ProblemHackTestcaseForJudge]] =
    HackJudgeTable.listProblemHackTestcases(connection, problemId).map {
      _.map(record =>
        ProblemHackTestcaseForJudge(
          hackId = record.hackId,
          subtaskIndex = record.subtaskIndex,
          inputRef = record.inputRef,
          answerRef = record.answerRef
        )
      )
    }
