package domains.rating.utils

import domains.user.objects.Username

object RatingCalculator:
  val particleCount: Int = 1000
  val minimumM: Int = 2
  val maximumM: Int = 100
  val defaultM: Int = 60
  val initialRating: Double = 1500.0

  final case class RankedParticipant(
    username: Username,
    rank: Int
  )

  final case class RatingContestEvent(
    participants: List[RankedParticipant],
    m: Int
  )

  lazy val initialGrowthBase: Double =
    solveInitialGrowthBase()

  lazy val initialParticles: Vector[Double] =
    (0 until particleCount).map(index => 2.4 * index + math.pow(initialGrowthBase, index.toDouble)).toVector

  def isValidM(m: Int): Boolean =
    m >= minimumM && m <= maximumM

  def percentileSlots(m: Int): Vector[Int] =
    require(isValidM(m), s"Rating m must be between $minimumM and $maximumM.")
    (0 until m).map(index => index * (particleCount - 1) / (m - 1)).toVector

  def ratingOf(particles: Vector[Double]): Double =
    particles.sum / particles.length.toDouble

  def applyContest(
    states: Map[Username, Vector[Double]],
    participants: List[RankedParticipant],
    m: Int
  ): Map[Username, Vector[Double]] =
    if participants.distinctBy(_.username).size < 2 then
      states
    else
      val slots = percentileSlots(m)
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

  def applyContestSequence(
    events: List[RatingContestEvent],
    existingUsers: Set[Username]
  ): Map[Username, Vector[Double]] =
    events.foldLeft(Map.empty[Username, Vector[Double]]) { (states, event) =>
      val activeParticipants = event.participants.filter(participant => existingUsers.contains(participant.username))
      applyContest(states, activeParticipants, event.m)
    }

  private def solveInitialGrowthBase(): Double =
    var low = 1.0
    var high = 1.02
    var iteration = 0
    while iteration < 100 do
      val mid = (low + high) / 2.0
      if initialParticleMean(mid) < initialRating then low = mid else high = mid
      iteration += 1
    (low + high) / 2.0

  private def initialParticleMean(base: Double): Double =
    (0 until particleCount).map(index => 2.4 * index + math.pow(base, index.toDouble)).sum / particleCount.toDouble

  private def selectedParticles(particles: Vector[Double], slots: Vector[Int]): Vector[Double] =
    slots.map(particles)

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
