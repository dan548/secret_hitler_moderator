package com.danpolyakov.telegram_bots.secret_hitler_moderator

enum class Symbols(private val hexStr : String, val description : String) {

    EMPTY_FIELD("\u25FB️️️️\uFE0F", "Empty field"),
    COVERED_FIELD("\u274C\uFE0F", "Field covered ith a card"),
    POLICY_PEEK("1F52E", "Presidential Power: Policy Peek"),
    INVEST("1F50D", "Presidential Power: Investigate Loyalty"),
    EXECUTE("1F5E1", "Presidential Power: Execution"),
    SPECIAL_ELECTION("1F0A1", "Presidential Power: Call Special Election"),
    LIB_WIN("1F54A", "Liberals win"),
    FASC_WIN("1F571", "Fascists win");

    private fun hexToByte(hexString : String) : Byte {
        val firstDigit = hexString[0].toInt()
        val secondDigit = hexString[1].toInt()
        return ((firstDigit shl 4) + secondDigit).toByte()
    }

    private fun hexStringToUTF32(hexString : String) : String {
        if (hexString.length % 2 == 0) {
            val byteArray = ByteArray(hexString.length / 2)
            for (i in 1..hexString.length / 2) {
                val substr = hexString.substring(2*(i-1), 2*i)
                byteArray[i-1] = hexToByte(substr)
            }
            return byteArray.toString(Charsets.UTF_32)
        } else {
            val byteArray = ByteArray((hexString.length + 1) / 2)
            val hexStringWithNulls = "0$hexString"
            for (i in 0..hexStringWithNulls.length / 2) {
                val substr = hexStringWithNulls.substring(2*i, 2*(i+1))
                byteArray[i] = hexToByte(substr)
            }
            return byteArray.toString(Charsets.UTF_32)
        }
    }

    fun getString() : String {
        return if (hexStr.length > 2) {
            hexStringToUTF32(hexStr)
        } else {
            hexStr
        }
    }
}