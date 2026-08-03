package ru.hukm.effectiveSpigot.utils

/** Combinatorial helpers. */
object EffectiveCombinator {
    /**
     * Cartesian product of [lists]: every way to pick one element from each inner list, preserving order.
     * The result size is the product of the inner list sizes.
     */
    fun <T> getAllCombinations(lists: List<List<T>>): List<List<T>> {
        if (lists.isEmpty()) return listOf(emptyList())

        val firstList = lists.first()
        val remainingLists = lists.drop(1)

        val remainingCombinations = getAllCombinations(remainingLists)

        return firstList.flatMap { element ->
            remainingCombinations.map { combination ->
                listOf(element) + combination
            }
        }
    }
}