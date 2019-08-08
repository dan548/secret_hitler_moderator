package com.danpolyakov.telegram_bots.secret_hitler_moderator

enum class Symbols(private val str : String, val description : String) {

    EMPTY_FIELD("\u25FB️️️️", "Empty field"),
    COVERED_FIELD("\u274C\uFE0F", "Field covered ith a card"),
    POLICY_PEEK("\uD83D\uDD2E", "Presidential Power: Policy Peek"),
    INVEST("\uD83D\uDD0D", "Presidential Power: Investigate Loyalty"),
    EXECUTE("\uD83D\uDDE1", "Presidential Power: Execution"),
    SPECIAL_ELECTION("\uD83C\uDCA1", "Presidential Power: Call Special Election"),
    LIB_WIN("\uD83D\uDD4A", "Liberals win"),
    FASC_WIN("\uD83D\uDD71", "Fascists win");


    fun getString() : String {
        return str
    }
}