package com.danpolyakov.telegram_bots.secret_hitler_moderator

data class State(var playerCounter : Int, var liberalTrack : Int = 0, var fascistTrack : Int = 0,
                 var failedVotes : Int = 0, var president : Player? = null,
                 var nominatedPresident : Player? = null, var nominatedChancellor : Player? = null,
                 var chosenPresident : Player? = null, var chancellor : Player? = null,
                 var dead : Int = 0, var lastVotes : MutableMap<Long, Long> = HashMap(),
                 var endCode : Int = 0, var drawnPolicies : MutableList<String> = ArrayList(),
                 var vetoRefused : Boolean = false)