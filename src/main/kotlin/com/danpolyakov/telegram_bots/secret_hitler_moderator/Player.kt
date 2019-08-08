package com.danpolyakov.telegram_bots.secret_hitler_moderator

class Player(val name : String, val userId : Long, var isDead : Boolean = false) {
    var role : String = "none"
        set(value) {
            party = if (value == "liberal") {
                "liberal"
            } else {
                "fascist"
            }
            field = value
        }
    lateinit var party : String
}