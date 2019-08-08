package com.danpolyakov.telegram_bots.secret_hitler_moderator

data class State(var liberalTrack : Int = 0, var fascistTrack : Int = 0,
                 var failedVotes : Int = 0, var president : Player? = null,
                 var nominatedPresident : Player? = null, var nominatedChancellor : Player? = null,
                 var chosenPresident : Player? = null, var chancellor : Player? = null,
                 var dead : Int = 0, var lastVotes : Map<Long, Long> = HashMap(),
                 var endCode : Int = 0, var drawnPolicies : List<String> = ArrayList(),
                 var vetoRefused : Boolean = false, var playerCounter : Int = 0)