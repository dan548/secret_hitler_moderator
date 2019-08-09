package com.danpolyakov.telegram_bots.secret_hitler_moderator

import java.lang.StringBuilder

class Board(val playerCount : Int, val game : Game) {
    val gameState : State = State(playerCount)
    private val fascistTrackActions : MutableList<String?> = ArrayList(6)
    var policiesLeft : Int = 17
    val pile : MutableList<String> = ArrayList()

    fun init() {
        for (i in 1..6) {
            pile.add("liberal")
        }
        for (i in 1..11) {
            pile.add("fascist")
        }
        when (playerCount) {
            5 -> {
                fascistTrackActions.addAll(listOf(null, null, "policy", "kill", "kill", "win"))
            }
            6 -> {
                fascistTrackActions.addAll(listOf(null, null, "policy", "kill", "kill", "win"))
                gameState.drawnPolicies.add("fascist")
                policiesLeft--
                pile -= pile.last()
            }
            7 -> {
                fascistTrackActions.addAll(listOf(null, "inspect", "choose", "kill", "kill", "win"))
                policiesLeft--
                pile -= pile.last()
            }
            8 -> {
                fascistTrackActions.addAll(listOf(null, "inspect", "choose", "kill", "kill", "win"))
            }
            9 -> {
                fascistTrackActions.addAll(listOf("inspect", "inspect", "choose", "kill", "kill", "win"))
                policiesLeft -= 2
                pile -= pile.last()
                pile -= pile.last()
            }
            10 -> {
                fascistTrackActions.addAll(listOf("inspect", "inspect", "choose", "kill", "kill", "win"))
            }
        }
    }

    fun print() : String {
        val builder : StringBuilder = StringBuilder("--- Liberal acts ---\n")
        for (i in 0..4) {
            if (i < gameState.liberalTrack) {
                builder.append(Symbols.COVERED_FIELD).append(' ')
            } else {
                if (i == 4) {
                    builder.append(Symbols.LIB_WIN).append(' ')
                } else {
                    builder.append(Symbols.EMPTY_FIELD).append(' ')
                }
            }
        }
        builder.append("\n--- Fascist acts ---\n")
        for (i in 0..5) {
            if (i < gameState.liberalTrack) {
                builder.append(Symbols.COVERED_FIELD).append(' ')
            } else {
                when (fascistTrackActions[i]) {
                    null -> builder.append(Symbols.EMPTY_FIELD).append(' ')
                    "policy" -> builder.append(Symbols.POLICY_PEEK).append(' ')
                    "inspect" -> builder.append(Symbols.INVEST).append(' ')
                    "choose" -> builder.append(Symbols.SPECIAL_ELECTION).append(' ')
                    "kill" -> builder.append(Symbols.EXECUTE).append(' ')
                    "win" -> builder.append(Symbols.FASC_WIN).append(' ')
                }
            }
        }
        builder.append("\n--- Election counter ---\n")
        for (i in 0..2) {
            if (i < gameState.failedVotes) {
                builder.append(Symbols.COVERED_FIELD).append(' ')
            } else {
                builder.append(Symbols.EMPTY_FIELD).append(' ')
            }
        }
        builder.append("\n--- Presidential order ---\n")
        game.mapNumberToPlayerId.forEach { (number, id) ->
            builder.append("$number. ${game.playerList[id]!!.name}\n")
        }
        builder.append("\nThere are $policiesLeft policies left on the pile.")
        if (gameState.fascistTrack >= 3) {
            builder.append("\n\nBeware: If Hitler gets elected as Chancellor the fascists win the game!")
        }
        return builder.toString()
    }
}