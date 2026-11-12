package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GoalRewardsTest {
    @Test fun milestoneChestTierMatchesAuraRungRules() {
        assertEquals(GoalRewards.Tier.COMMON, GoalRewards.tierForLeg(0, 3))
        assertEquals(GoalRewards.Tier.RARE, GoalRewards.tierForLeg(0, 7))
        assertEquals(GoalRewards.Tier.EPIC, GoalRewards.tierForLeg(7, 20))
        assertEquals(GoalRewards.Tier.LEGENDARY, GoalRewards.tierForLeg(0, 365))
    }

    @Test fun rollIsStableAndOwnedItemsAreExcluded() {
        val chest = GoalRewards.Chest("ch-fixed-7", GoalRewards.Tier.LEGENDARY, "Day 365", 1)
        val first = GoalRewards.roll(chest, emptySet(), freezesHeld = 2)
        assertEquals(first, GoalRewards.roll(chest, emptySet(), freezesHeld = 2))
        val ownedTarget = when (first) {
            is GoalRewards.Reward.Item -> setOf(first.id)
            is GoalRewards.Reward.Freeze -> return
        }
        val reroll = GoalRewards.roll(chest, ownedTarget, freezesHeld = 2)
        assertNotEquals(first, reroll)
    }

    @Test fun freezeAwardRespectsTheRemainingCapacity() {
        val chest = GoalRewards.Chest("ch-freeze-cap", GoalRewards.Tier.LEGENDARY, "Day 1000", 1)
        repeat(100) {
            val reward = GoalRewards.roll(chest.copy(id = "ch-freeze-cap-$it"), emptySet(), freezesHeld = 1)
            if (reward is GoalRewards.Reward.Freeze) assertEquals(1, reward.count)
        }
    }
}
