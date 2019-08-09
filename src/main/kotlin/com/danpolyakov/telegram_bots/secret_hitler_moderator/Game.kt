package com.danpolyakov.telegram_bots.secret_hitler_moderator

import me.ivmg.telegram.Bot
import java.lang.StringBuilder
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
        board = Board(playerList.keys.size, this)
        board!!.init()
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

    fun informPlayers(bot : Bot) {
        val hitler = getHitler()
        for (player in playerList.values) {
            bot.sendMessage(player.userId, "Your secret role: ${player.role}\n" +
                    "Your party membership: ${player.party}")
        }
        val list = getFascists()
        val fascInfo = list.fold(StringBuilder()) { builder, fascist ->
            builder.append(' ').append(fascist.name)
        }.toString()
        for (fascist in list) {
            bot.sendMessage(fascist.userId, "Fascists are:$fascInfo")
            bot.sendMessage(fascist.userId, "Hitler is: ${hitler.name}")
        }
        if (board!!.playerCount == 5 || board!!.playerCount == 6) {
            bot.sendMessage(hitler.userId, "Your fascist is:$fascInfo")
        }
    }

    private fun getHitler() : Player = playerList.values.first { player -> player.role == "Hitler" }
    private fun getFascists() : List<Player> = playerList.values.filter { player -> player.role == "fascist" }
}