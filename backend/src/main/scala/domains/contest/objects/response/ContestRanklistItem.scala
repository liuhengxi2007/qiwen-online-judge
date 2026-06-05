package domains.contest.objects.response

import domains.contest.objects.{ContestPenaltyMillis, ContestRank, ContestScore}
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ContestRanklistItem(
  rank: ContestRank,
  user: UserIdentity,
  totalScore: ContestScore,
  penaltyMillis: ContestPenaltyMillis,
  problemResults: List[ContestRanklistProblemResult]
)

object ContestRanklistItem:
  given Encoder[ContestRanklistItem] = deriveEncoder[ContestRanklistItem]
  given Decoder[ContestRanklistItem] = deriveDecoder[ContestRanklistItem]
