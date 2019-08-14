package com.danpolyakov.telegram_bots.secret_hitler_moderator

import me.ivmg.telegram.Bot
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class Game(val chatId : Long, val initiatorId : Long) {

    private val playerList : MutableList<Player> = ArrayList()
    lateinit var board : Board
    private val mapIdToPlayerNumber : MutableMap<Long, Int> = HashMap()
    private val mapIdToPlayer : MutableMap<Long, Player> = HashMap()

    fun getPlayerById(id : Long?) : Player? = mapIdToPlayer[id]

    fun getNumberById(id : Long?) : Int? = id?.let { mapIdToPlayerNumber[it] }

    private fun getPlayerByNumber(number : Int?) : Player? = number?.let { playerList[it - 1] }

    fun killPlayer(id : Long?) {
        mapIdToPlayerNumber.remove(id)
    }

    fun addPlayer(player : Player) {
        if (!playerList.contains(player)) {
            playerList.add(player)
            mapIdToPlayer.putIfAbsent(player.userId, player)
        }
    }

    fun start() {
        playerList.shuffle()

        for ((index, player) in playerList.withIndex()) {
            mapIdToPlayerNumber[player.userId] = index + 1
        }

        giveRoles()
        board = Board(playerList.size, this)
        board.init()
    }

    fun isStarted() : Boolean {
        return ::board.isInitialized
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
        for (i in 0 until playersCount) {
            playerList[i].role = roles[i]
        }
    }

    fun informPlayers(bot : Bot) {
        val hitler = getHitler()
        for (player in playerList) {
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
        if (board.playerCount == 5 || board.playerCount == 6) {
            bot.sendMessage(hitler.userId, "Your fascist is:$fascInfo")
        }
    }

    fun printRoles() : String {
        val builder = StringBuilder()
        if (isStarted()) {
            for (p in playerList) {
                builder.append("${p.name}'s secret role was ${p.role}.\n")
            }
        }
        return builder.toString()
    }

    private fun getHitler() : Player = playerList.first { player -> player.role == "Hitler" }
    private fun getFascists() : List<Player> = playerList.filter { player -> player.role == "fascist" }
    fun getAlive() : List<Player> = mapIdToPlayerNumber.values.map { num -> getPlayerByNumber(num)!! }
    fun state() : State = board.gameState
    fun getAll() : List<Player> = mapIdToPlayer.values.toList()
}
