package domains.rating.utils

import domains.rating.utils.RatingCalculator.RankedParticipant
import domains.user.objects.Username
import munit.FunSuite

class RatingCalculatorSuite extends FunSuite:

  private val alice = Username.canonical("alice")
  private val bob = Username.canonical("bob")

  test("initial particles have rating mean near 1500") {
    assertEqualsDouble(RatingCalculator.initialGrowthBase, 1.0077914200183185, 1e-12)
    assertEqualsDouble(RatingCalculator.ratingOf(RatingCalculator.initialParticles), 1500.0, 1e-9)
  }

  test("percentile slots use floor interpolation for supported m values") {
    assertEquals(RatingCalculator.percentileSlots(2), Vector(0, 999))
    assertEquals(RatingCalculator.percentileSlots(60).take(5), Vector(0, 16, 33, 50, 67))
    assertEquals(RatingCalculator.percentileSlots(60).drop(55), Vector(931, 948, 965, 982, 999))
    assertEquals(RatingCalculator.percentileSlots(100).take(3), Vector(0, 10, 20))
    assertEquals(RatingCalculator.percentileSlots(100).takeRight(2), Vector(988, 999))
  }

  test("single contest assigns stronger percentile particles to higher non-tied ranks") {
    val states = RatingCalculator.applyContest(
      states = Map.empty,
      participants = List(RankedParticipant(alice, 1), RankedParticipant(bob, 2)),
      m = 2
    )

    assert(states.contains(alice))
    assert(states.contains(bob))
    assert(RatingCalculator.ratingOf(states(alice)) > RatingCalculator.initialRating)
    assert(RatingCalculator.ratingOf(states(bob)) < RatingCalculator.initialRating)
  }

  test("tied participants share averaged percentile particles") {
    val states = RatingCalculator.applyContest(
      states = Map.empty,
      participants = List(RankedParticipant(alice, 1), RankedParticipant(bob, 1)),
      m = 2
    )

    assertEquals(states(alice), RatingCalculator.initialParticles)
    assertEquals(states(bob), RatingCalculator.initialParticles)
  }

  test("replacement removes percentile slots and resorts all particles") {
    val aliceParticles = (0 until RatingCalculator.particleCount).map(_.toDouble).toVector
    val bobParticles = (0 until RatingCalculator.particleCount).map(index => (1000 + index).toDouble).toVector

    val states = RatingCalculator.applyContest(
      states = Map(alice -> aliceParticles, bob -> bobParticles),
      participants = List(RankedParticipant(alice, 1), RankedParticipant(bob, 2)),
      m = 2
    )

    assertEquals(states(alice).head, 1.0)
    assertEquals(states(alice).takeRight(2), Vector(1000.0, 1999.0))
    assertEquals(states(bob).head, 0.0)
    assertEquals(states(bob).last, 1998.0)
    assertEquals(states(alice).size, RatingCalculator.particleCount)
    assertEquals(states(bob).size, RatingCalculator.particleCount)
  }
