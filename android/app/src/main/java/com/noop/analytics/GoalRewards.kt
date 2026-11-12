package com.noop.analytics

/** On-device milestone chest rules ported from StrandFitness.Rewards. */
object GoalRewards {
    enum class Tier { COMMON, RARE, EPIC, LEGENDARY }
    sealed interface Reward {
        data class Freeze(val count: Int) : Reward
        data class Item(val id: String) : Reward
    }
    data class Item(val id: String, val rarity: Tier)
    data class Chest(
        val id: String,
        val tier: Tier,
        val reason: String,
        val earnedEpochSecond: Long,
        val openedEpochSecond: Long? = null,
        val reward: Reward? = null,
    )

    val catalog = listOf(
        Item("sunset", Tier.COMMON), Item("citrus", Tier.COMMON),
        Item("ocean", Tier.RARE), Item("jade", Tier.RARE), Item("rose", Tier.RARE),
        Item("ice", Tier.EPIC), Item("gold", Tier.EPIC), Item("aurora", Tier.LEGENDARY), Item("void", Tier.LEGENDARY),
        Item("orb-dawn", Tier.COMMON), Item("orb-lagoon", Tier.COMMON), Item("orb-orchid", Tier.RARE),
        Item("orb-cinder", Tier.RARE), Item("orb-gold", Tier.EPIC), Item("orb-void", Tier.LEGENDARY),
        Item("icon-ember", Tier.RARE), Item("icon-frost", Tier.EPIC), Item("icon-aurora", Tier.LEGENDARY),
        Item("widget-fire", Tier.RARE), Item("widget-road", Tier.EPIC),
        Item("face-coach", Tier.COMMON), Item("face-glasses", Tier.RARE),
        Item("face-nightcap", Tier.RARE), Item("face-crown", Tier.LEGENDARY),
    )
    const val FREEZE_CAP = 2

    fun tierForLeg(base: Int, target: Int): Tier {
        if (target <= StreakChallenge.comeback) return Tier.COMMON
        val rungCount = StreakChallenge.ladder.count { it > base && it <= target }
        val size = if (rungCount >= 2) Tier.EPIC else Tier.RARE
        return if (target % 365 == 0 || target % 100 == 0) Tier.entries[(size.ordinal + 1).coerceAtMost(Tier.LEGENDARY.ordinal)] else size
    }

    fun roll(chest: Chest, owned: Set<String>, freezesHeld: Int): Reward {
        val rng = SplitMix64(stableHash(chest.id))
        val freezeCount = if (chest.tier >= Tier.EPIC) 2 else 1
        val freezeOdds = listOf(.30, .30, .35, .30)[chest.tier.ordinal]
        if (freezesHeld < FREEZE_CAP && rng.unit() < freezeOdds) return Reward.Freeze(minOf(freezeCount, FREEZE_CAP - freezesHeld))
        val unowned = catalog.filterNot { it.id in owned }
        if (unowned.isEmpty()) return Reward.Freeze(freezeCount)
        val weights = listOf(
            listOf(70.0, 25.0, 5.0, 0.0), listOf(30.0, 50.0, 18.0, 2.0),
            listOf(0.0, 40.0, 45.0, 15.0), listOf(0.0, 0.0, 50.0, 50.0),
        )[chest.tier.ordinal]
        var pick = rng.unit() * weights.sum()
        var rarity = Tier.LEGENDARY
        for (tier in Tier.entries) {
            if (pick < weights[tier.ordinal]) { rarity = tier; break }
            pick -= weights[tier.ordinal]
        }
        val order = Tier.entries.sortedWith(compareBy<Tier> { kotlin.math.abs(it.ordinal - rarity.ordinal) }.thenByDescending { it.ordinal })
        for (tier in order) {
            val pool = unowned.filter { it.rarity == tier }
            if (pool.isNotEmpty()) return Reward.Item(pool[(rng.unit() * pool.size).toInt() % pool.size].id)
        }
        return Reward.Freeze(freezeCount)
    }

    fun stableHash(value: String): ULong {
        var h = 0xcbf29ce484222325uL
        value.encodeToByteArray().forEach { b -> h = (h xor (b.toUByte().toULong())) * 0x100000001b3uL }
        return h
    }

    private class SplitMix64(seed: ULong) {
        private var state = seed
        fun next(): ULong {
            state += 0x9E3779B97F4A7C15uL
            var z = state
            z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
            z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
            return z xor (z shr 31)
        }
        fun unit(): Double = (next() shr 11).toDouble() / 9007199254740992.0
    }
}
