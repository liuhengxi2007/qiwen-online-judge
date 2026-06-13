package domains.submission.objects

import domains.contest.objects.{ContestSlug, ContestTitle}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 提交来源；None 表示题库/题单提交，Some 表示来自某场竞赛。 */
final case class SubmissionSource(
  contestSlug: Option[ContestSlug],
  contestTitle: Option[ContestTitle]
)

/** 提交来源构造与 JSON 编解码工具。 */
object SubmissionSource:
  val FromProblemSet: SubmissionSource = SubmissionSource(contestSlug = None, contestTitle = None)

  /** 构造竞赛来源，用于竞赛提交列表和详情展示。 */
  def fromContest(slug: ContestSlug, title: ContestTitle): SubmissionSource =
    SubmissionSource(contestSlug = Some(slug), contestTitle = Some(title))

  given Encoder[SubmissionSource] = deriveEncoder[SubmissionSource]
  given Decoder[SubmissionSource] = deriveDecoder[SubmissionSource]
