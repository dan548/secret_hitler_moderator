package com.danpolyakov.telegram_bots.secret_hitler_moderator

import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.text

fun main() {

    val bot = bot {

        token = "926152253:AAEZc96FuDs7MsmV8iBK3sFx3eEbFgi8JZ8"
        dispatch {
            text { bot, update ->
                val text = update.message?.text ?: "Hello, World!"
                bot.sendMessage(chatId = update.message!!.chat.id, text = text)
            }
            command("greet") { bot, update ->
                val firstName = update.message!!.forwardFrom?.firstName
                bot.sendMessage(chatId = update.message!!.chat.id, text = "$firstName Украине!")
            }
        }
    }
    bot.startPolling()
}