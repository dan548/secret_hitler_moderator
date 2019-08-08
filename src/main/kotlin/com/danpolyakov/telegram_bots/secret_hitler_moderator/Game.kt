package com.danpolyakov.telegram_bots.secret_hitler_moderator

import java.util.*
import kotlin.collections.HashMap

class Game(val chatId : Long, val initiatorId : Long) {

    val playerList : MutableMap<Long, Player> = HashMap()
    var board : Board? = null
    val mapNumberToPlayerId : MutableMap<Int, Long> = HashMap()

    fun addPlayer(userId : Long, player : Player) {
        playerList.putIfAbsent(userId, player)
    }

    fun start() {
        val random = Random()
        val keys = playerList.keys.shuffled(random)
        for ((index, key) in keys.withIndex()) {
            mapNumberToPlayerId[index + 1] = key
        }
        giveRoles()
    }

    private fun giveRoles() {
        val playersCount = playerList.size
        val fascistCount = (playersCount - 1) / 2
        val roles = ArrayList<String>()
        for (i in 1 until fascistCount) {
            roles.add("fascist")
        }
        roles.add("Hitler")
        for (i in 1..playersCount - fascistCount) {
            roles.add("liberal")
        }
        roles.shuffle()
        for ((index, role) in roles.withIndex()) {
            val id = mapNumberToPlayerId[index]
            playerList[id]!!.role = role
        }
    }

    fun getHitler() : Player = playerList.values.first { player -> player.role == "Hitler" }
    fun getFascists() : List<Player> = playerList.values.filter { player -> player.role == "fascist" }
}