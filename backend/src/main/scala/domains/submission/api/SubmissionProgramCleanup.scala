package domains.submission.api

import cats.effect.IO
import cats.syntax.all.*
import domains.problem.objects.ProblemId
import domains.submission.table.submission.SubmissionQueryTable

import java.sql.Connection

/** 提交程序对象清理辅助；用于删除题目前先收集需要删除的源码对象。 */
object SubmissionProgramCleanup:

  /** 预先读取题目下所有提交 manifest，返回稍后执行的对象存储 best-effort 删除动作。 */
  def prepareDeleteForProblem(
    connection: Connection,
    problemId: ProblemId,
    submissionProgramStorage: SubmissionProgramStorageContext
  ): IO[IO[Unit]] =
    SubmissionQueryTable
      .listProgramManifestsForProblem(connection, problemId)
      // 注意：清理提交程序对象失败不会阻断题目删除，后续需要独立的孤儿对象回收机制兜底。
      .map(_.traverse_(manifest => SubmissionProgramStorage.deleteManifest(submissionProgramStorage, manifest).handleError(_ => ())))
