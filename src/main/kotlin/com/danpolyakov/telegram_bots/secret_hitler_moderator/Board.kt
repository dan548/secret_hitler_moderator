package com.danpolyakov.telegram_bots.secret_hitler_moderator

class Board(val playerCount : Int, val game : Game) {
    val gameState : State = State()
    val fascistTrackActions : MutableList<String?> = ArrayList(6)

    private fun initActions() {
        when (playerCount) {
            5 -> {
                fascistTrackActions.addAll(listOf(null, null, "policy", "kill", "kill", "win"))
            }
            6 -> {
                fascistTrackActions.addAll(listOf(null, null, "policy", "kill", "kill", "win"))
            }
            7 -> {
                fascistTrackActions.addAll(listOf(null, "inspect", "choose", "kill", "kill", "win"))
            }
            8 -> {
                fascistTrackActions.addAll(listOf(null, "inspect", "choose", "kill", "kill", "win"))
            }
            9 -> {
                fascistTrackActions.addAll(listOf("inspect", "inspect", "choose", "kill", "kill", "win"))
            }
            10 -> {
                fascistTrackActions.addAll(listOf("inspect", "inspect", "choose", "kill", "kill", "win"))
            }
        }
    }

    fun print() {

    }
}