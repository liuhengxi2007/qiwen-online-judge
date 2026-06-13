package domains.rating.utils

import domains.user.objects.Username

/** 粒子制评分计算器，按比赛排名替换用户评分分布中的百分位粒子。 */
object RatingCalculator:
  val particleCount: Int = 1000
  val minimumM: Int = 2
  val maximumM: Int = 100
  val defaultM: Int = 60
  val initialRating: Double = 1500.0

  /** 评分计算输入中的参赛者，rank 越小排名越高。 */
  final case class RankedParticipant(
    username: Username,
    rank: Int
  )

  /** 一场纳入评分序列的比赛事件，包含参赛者排名和本场替换粒子数量 m。 */
  final case class RatingContestEvent(
    participants: List[RankedParticipant],
    m: Int
  )

  lazy val initialGrowthBase: Double =
    solveInitialGrowthBase()

  lazy val initialParticles: Vector[Double] =
    (0 until particleCount).map(index => 2.4 * index + math.pow(initialGrowthBase, index.toDouble)).toVector

  /** 判断 m 是否处于允许的替换粒子数量范围。 */
  def isValidM(m: Int): Boolean =
    m >= minimumM && m <= maximumM

  /** 计算 m 个待替换百分位槽位，m 非法时抛出 require 异常。 */
  def percentileSlots(m: Int): Vector[Int] =
    require(isValidM(m), s"Rating m must be between $minimumM and $maximumM.")
    (0 until m).map(index => index * (particleCount - 1) / (m - 1)).toVector

  /** 将用户粒子分布转换为展示 rating，当前使用粒子均值。 */
  def ratingOf(particles: Vector[Double]): Double =
    particles.sum / particles.length.toDouble

  /** 应用单场比赛结果，返回替换参赛者粒子后的评分状态；少于两个去重参赛者时不变。 */
  def applyContest(
    states: Map[Username, Vector[Double]],
    participants: List[RankedParticipant],
    m: Int
  ): Map[Username, Vector[Double]] =
    if participants.distinctBy(_.username).size < 2 then
      states
    else
      val slots = percentileSlots(m)
      /** 注意：同一用户名重复出现在参赛者列表时只保留第一次，避免单个用户在同场比赛中重复参与分配。 */
      val orderedParticipants = participants.distinctBy(_.username).sortBy(participant => (participant.rank, participant.username.value)).toVector
      val pool = orderedParticipants
        .flatMap(participant => selectedParticles(states.getOrElse(participant.username, initialParticles), slots))
        .sortWith(_ > _)
        .toVector
      val assignments = assignParticles(orderedParticipants, pool, m)
      assignments.foldLeft(states) { case (currentStates, (username, assignedParticles)) =>
        val currentParticles = currentStates.getOrElse(username, initialParticles)
        currentStates.updated(username, replacePercentileParticles(currentParticles, slots, assignedParticles))
      }

  /** 按比赛追加顺序重放评分序列，并忽略已不存在的用户。 */
  def applyContestSequence(
    events: List[RatingContestEvent],
    existingUsers: Set[Username]
  ): Map[Username, Vector[Double]] =
    events.foldLeft(Map.empty[Username, Vector[Double]]) { (states, event) =>
      val activeParticipants = event.participants.filter(participant => existingUsers.contains(participant.username))
      applyContest(states, activeParticipants, event.m)
    }

  /** 通过二分求初始粒子增长基数，使初始粒子均值贴近默认评分。 */
  private def solveInitialGrowthBase(): Double =
    var low = 1.0
    var high = 1.02
    var iteration = 0
    while iteration < 100 do
      val mid = (low + high) / 2.0
      if initialParticleMean(mid) < initialRating then low = mid else high = mid
      iteration += 1
    (low + high) / 2.0

  /** 计算给定增长基数下初始粒子池的平均评分。 */
  private def initialParticleMean(base: Double): Double =
    (0 until particleCount).map(index => 2.4 * index + math.pow(base, index.toDouble)).sum / particleCount.toDouble

  /** 按排名槽位从用户粒子状态中选出参与本场计算的粒子。 */
  private def selectedParticles(particles: Vector[Double], slots: Vector[Int]): Vector[Double] =
    slots.map(particles)

  /** 根据本场排名和 m 值重新分配粒子，输出每名参赛者的新粒子片段。 */
  private def assignParticles(
    participants: Vector[RankedParticipant],
    pool: Vector[Double],
    m: Int
  ): Map[Username, Vector[Double]] =
    val assignments = Map.newBuilder[Username, Vector[Double]]
    var offset = 0
    groupedByRank(participants).foreach { group =>
      val groupSize = group.size
      val segment = pool.slice(offset, offset + groupSize * m)
      val assignedParticles =
        if groupSize == 1 then
          segment
        else
          (0 until m).map { index =>
            val sharedParticles = segment.slice(index * groupSize, (index + 1) * groupSize)
            sharedParticles.sum / groupSize.toDouble
          }.toVector
      group.foreach(participant => assignments += participant.username -> assignedParticles)
      offset += groupSize * m
    }
    assignments.result()

  private def groupedByRank(participants: Vector[RankedParticipant]): Vector[Vector[RankedParticipant]] =
    participants.foldLeft(Vector.empty[Vector[RankedParticipant]]) { (groups, participant) =>
      groups.lastOption match
        case Some(group) if group.head.rank == participant.rank =>
          groups.init :+ (group :+ participant)
        case _ =>
          groups :+ Vector(participant)
    }

  private def replacePercentileParticles(
    particles: Vector[Double],
    slots: Vector[Int],
    assignedParticles: Vector[Double]
  ): Vector[Double] =
    val replacedSlots = slots.toSet
    particles.zipWithIndex.collect {
      case (particle, index) if !replacedSlots.contains(index) => particle
    }.appendedAll(assignedParticles).sorted
