package judger.infra

import judgeprotocol.objects.SubmissionVerdict
import judgeprotocol.objects.response.*

object JudgeResultAggregator:
  private final case class ResultTrees[A](base: A, worst: A):
    def map[B](f: A => B): ResultTrees[B] =
      ResultTrees(f(base), f(worst))

    def zip[B](other: ResultTrees[B]): ResultTrees[(A, B)] =
      ResultTrees(base -> other.base, worst -> other.worst)

  private final case class ScoreAggregation(
    scores: ResultTrees[BigDecimal],
    verdictChildren: ResultTrees[List[(BigDecimal, SubmissionVerdict)]]
  )

  private[infra] def aggregateSubtask(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): JudgeSubtaskResult =
    val baseTestcases = mainTestcases(testcases)
    val scoreAggregation = aggregateTestcaseScores(subtask, testcases)
    val metrics = scoreAggregation.scores
      .zip(scoreAggregation.verdictChildren)
      .zip(
        ResultTrees(
          base = UsageSummary(
            aggregateUsage(subtask.aggregation.time, baseTestcases.flatMap(_.timeUsedMs)),
            aggregateUsage(subtask.aggregation.memory, baseTestcases.flatMap(_.memoryUsedKb))
          ),
          worst = worstSubtaskUsage(subtask, testcases)
        )
      )
      .zip(ResultTrees(base = baseTestcases, worst = testcases))
      .map { case (((score, verdictChildren), usage), summaryTestcases) =>
        val verdict = aggregateVerdict(score, verdictChildren)
        resultSummary(
          score = score,
          verdict = verdict,
          reason = reasonForTestcases(verdict, summaryTestcases),
          timeUsedMs = usage.timeUsedMs,
          memoryUsedKb = usage.memoryUsedKb
        )
      }
    JudgeSubtaskResult(
      subtask.index,
      subtask.label,
      metrics.base,
      metrics.worst,
      testcases
    )

  private[infra] def aggregateTask(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): JudgeResult =
    val scoreAggregation = aggregateSubtaskScores(task, subtasks)
    val scores = scoreAggregation.scores.map(score => roundFinalScore(score, task.roundingScale))
    val metrics = scores
      .zip(scoreAggregation.verdictChildren)
      .zip(ResultTrees(base = subtasks.map(_.baseResult), worst = subtasks.map(_.worstResult)))
      .map { case ((score, verdictChildren), childSummaries) =>
        val verdict = aggregateVerdict(score, verdictChildren)
        resultSummary(
          score = score,
          verdict = verdict,
          reason = reasonForSummaries(verdict, childSummaries),
          timeUsedMs = aggregateUsage(task.aggregation.time, childSummaries.flatMap(_.timeUsedMs)),
          memoryUsedKb = aggregateUsage(task.aggregation.memory, childSummaries.flatMap(_.memoryUsedKb))
        )
      }
    JudgeResult(
      metrics.base,
      metrics.worst,
      subtasks
    )

  private def aggregateTestcaseScores(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): ScoreAggregation =
    val baseTestcases = mainTestcases(testcases)
    val worstCaseScore = worstScore(testcases.map(result => result.score -> result.verdict))
    val baseScore = aggregateScore(subtask.aggregation.score, baseTestcases.map(_.score), testcaseScoreRatios(subtask, baseTestcases))
    ScoreAggregation(
      ResultTrees(base = baseScore, worst = worstCaseScore),
      ResultTrees(
        base = baseTestcases.map(result => result.score -> result.verdict),
        worst = testcases.map(result => result.score -> result.verdict)
      )
    )

  private def aggregateSubtaskScores(task: JudgeTask, subtasks: List[JudgeSubtaskResult]): ScoreAggregation =
    val baseScore = aggregateScore(task.aggregation.score, subtasks.map(_.baseResult.score), task.subtasks.map(_.scoreRatio))
    val worstScoreValue = aggregateScore(task.aggregation.score, subtasks.map(_.worstResult.score), task.subtasks.map(_.scoreRatio))
    ScoreAggregation(
      ResultTrees(base = baseScore, worst = worstScoreValue),
      ResultTrees(
        base = subtasks.map(result => result.baseResult.score -> result.baseResult.verdict),
        worst = subtasks.map(result => result.worstResult.score -> result.worstResult.verdict)
      )
    )

  private def testcaseScoreRatios(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): List[BigDecimal] =
    testcases.map(result => subtask.testcases.find(_.index == result.index).map(_.scoreRatio).getOrElse(BigDecimal(1)))

  private def mainTestcases(testcases: List[JudgeTestcaseResult]): List[JudgeTestcaseResult] =
    testcases.filter(_.testcaseType == JudgeTestcaseType.Main)

  private final case class UsageSummary(timeUsedMs: Option[Long], memoryUsedKb: Option[Long])

  private def worstSubtaskUsage(subtask: JudgeTaskSubtask, testcases: List[JudgeTestcaseResult]): UsageSummary =
    val timeUsedMs =
      testcases.flatMap(_.timeUsedMs).maxOption.map { maxTime =>
        if subtask.aggregation.time == "sum" then maxTime * mainTestcases(testcases).size
        else maxTime
      }
    UsageSummary(
      timeUsedMs = timeUsedMs,
      memoryUsedKb = testcases.flatMap(_.memoryUsedKb).maxOption
    )

  private def resultSummary(
    score: BigDecimal,
    verdict: SubmissionVerdict,
    reason: Option[JudgeFailureReason],
    timeUsedMs: Option[Long],
    memoryUsedKb: Option[Long]
  ): JudgeResultSummary =
    val normalizedVerdict = JudgeResultSummary.normalizeNodeVerdict(verdict)
    if normalizedVerdict == SubmissionVerdict.SystemError then
      JudgeResultSummary.systemError(score, reason.getOrElse(JudgeFailureReason.SystemError), timeUsedMs, memoryUsedKb)
    else
      JudgeResultSummary.nonSystem(score, normalizedVerdict, timeUsedMs, memoryUsedKb)

  private def reasonForTestcases(verdict: SubmissionVerdict, testcases: List[JudgeTestcaseResult]): Option[JudgeFailureReason] =
    Option.when(verdict == SubmissionVerdict.SystemError)(
      testcases.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )

  private def reasonForSummaries(verdict: SubmissionVerdict, summaries: List[JudgeResultSummary]): Option[JudgeFailureReason] =
    Option.when(verdict == SubmissionVerdict.SystemError)(
      summaries.find(_.verdict == SubmissionVerdict.SystemError).flatMap(_.reason).getOrElse(JudgeFailureReason.SystemError)
    )

  private def worstScore(children: List[(BigDecimal, SubmissionVerdict)]): BigDecimal =
    children.map(_._1).minOption.getOrElse(BigDecimal(0))

  private def aggregateScore(kind: String, scores: List[BigDecimal], ratios: List[BigDecimal]): BigDecimal =
    kind match
      case "min" => if scores.isEmpty then BigDecimal(0) else scores.min
      case "sum" => scores.zip(ratios).map { case (score, ratio) => score * ratio }.sum
      case _ => BigDecimal(0)

  private def aggregateUsage(kind: String, values: List[Long]): Option[Long] =
    if values.isEmpty then None
    else
      kind match
        case "sum" => Some(values.sum)
        case _ => Some(values.max)

  private def aggregateVerdict(score: BigDecimal, children: List[(BigDecimal, SubmissionVerdict)]): SubmissionVerdict =
    if score == BigDecimal(1) && children.exists(_._2 == SubmissionVerdict.AcceptedByProtocol) then SubmissionVerdict.AcceptedByProtocol
    else if score == BigDecimal(1) then SubmissionVerdict.Accepted
    else children.minByOption(_._1).map(_._2).getOrElse(SubmissionVerdict.SystemError)

  private def roundFinalScore(score: BigDecimal, scale: Int): BigDecimal =
    val roundedUp = score.setScale(scale, BigDecimal.RoundingMode.CEILING)
    if score < BigDecimal(1) && roundedUp >= BigDecimal(1) then BigDecimal(1) - BigDecimal(java.math.BigDecimal.ONE.movePointLeft(scale))
    else roundedUp

  private[infra] def clampScore(score: BigDecimal): BigDecimal =
    score.max(BigDecimal(0)).min(BigDecimal(1))

  private[infra] def containsSystemError(result: JudgeResult): Boolean =
    result.baseResult.verdict == SubmissionVerdict.SystemError ||
      result.worstResult.verdict == SubmissionVerdict.SystemError ||
      result.subtasks.exists(subtask =>
        subtask.baseResult.verdict == SubmissionVerdict.SystemError ||
          subtask.worstResult.verdict == SubmissionVerdict.SystemError ||
          subtask.testcases.exists(_.verdict == SubmissionVerdict.SystemError)
      )
