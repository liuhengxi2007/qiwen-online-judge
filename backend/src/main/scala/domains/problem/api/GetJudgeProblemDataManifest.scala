package domains.problem.api

import cats.effect.IO
import domains.auth.api.InternalOnlyApi
import domains.problem.objects.{ProblemId, ProblemSlug}
import domains.problem.objects.internal.ProblemDataManifest
import domains.problem.table.problem.ProblemQueryTable
import domains.problem.table.problem_data_file.ProblemDataFileTable
import org.http4s.Method
import shared.api.ApiPath

import java.sql.Connection

/** 判题侧读取题目数据清单的内部输入；同时携带 id 和 slug 用于防止 slug 复用造成错读。 */
final case class JudgeProblemDataManifestInput(
  problemId: ProblemId,
  problemSlug: ProblemSlug
)

/** 内部题目数据清单 API；判题和提交校验使用它读取数据库中已登记的数据文件版本。 */
object GetJudgeProblemDataManifest extends InternalOnlyApi[JudgeProblemDataManifestInput, Option[ProblemDataManifest]]:

  override val method: Method = Method.POST
  override val path: ApiPath = ApiPath("/api/internal/problems/judge-data-manifest")

  /** 构造内部请求输入；输出只封装 id/slug，不触发数据库访问。 */
  def input(problemId: ProblemId, problemSlug: ProblemSlug): JudgeProblemDataManifestInput =
    JudgeProblemDataManifestInput(problemId = problemId, problemSlug = problemSlug)

  /** 仅当 slug 当前仍指向同一个题目 id 时返回清单；否则返回 None 让调用方按题目不存在处理。 */
  override def plan(connection: Connection, input: JudgeProblemDataManifestInput): IO[Option[ProblemDataManifest]] =
    ProblemQueryTable.findBySlug(connection, input.problemSlug).flatMap {
      case Some(problem) if problem.id == input.problemId =>
        ProblemDataFileTable
          .manifestForProblem(connection, input.problemId, input.problemSlug)
          .map(Some(_))
      case _ =>
        IO.pure(None)
    }
