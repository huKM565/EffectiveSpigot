package ru.hukm.effectiveSpigot.utils

import org.bukkit.Material

object EffectiveCombinator {
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