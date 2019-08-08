package com.danpolyakov.telegram_bots.secret_hitler_moderator

import me.ivmg.telegram.Bot
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.InlineKeyboardButton

class Main {

    fun main() {

        val games = mutableMapOf<Long, Game>()

        val message = """
            "Тайный Гитлер - игра социальных вычетов для 5-10 человек о поиске и остановке Тайного Гитлера. Большинство игроков - либералы. Если они могут научиться доверять друг другу, у них достаточно голосов, чтобы контролировать стол и выигрывать игру. Но некоторые игроки - фашисты. Они скажут все, что нужно, чтобы быть избранными, принять свою повестку дня и обвинить других в последствиях. Либералы должны работать вместе, чтобы обнаружить правду, прежде чем фашисты установят своего хладнокровного лидера и выиграют игру ".
            |- Официальное описание Секретного Гитлера
            
            |Добавьте меня в группу и введите /newgame, чтобы создать игру!""".trimMargin()

        val commands = """Доступны следующие команды:
            |/help - Предоставление информации о доступных командах
            |/start - Показать сообщение и список команд""".trimMargin()

        val bot = bot {

            token = "926152253:AAEZc96FuDs7MsmV8iBK3sFx3eEbFgi8JZ8"
            dispatch {
                command("help") { bot, update ->
                    bot.sendMessage(chatId = update.message!!.chat.id, text = commands)
                }
                command("start") { bot, update ->
                    bot.sendMessage(chatId = update.message!!.chat.id, text = message)
                    bot.sendMessage(chatId = update.message!!.chat.id, text = commands)
                }
                command("rules") { bot, update ->
                    val btn = InlineKeyboardButton("Rules", "http://www.secrethitler.com/assets/Secret_Hitler_Rules.pdf")

                    bot.sendMessage(chatId = update.message!!.chat.id, text = "Read the official Secret Hitler rules:", replyMarkup = btn)
                }
                command("newgame") { bot, update ->
                    val chatId = update.message!!.chat.id
                    val chatType = update.message!!.chat.type

                    if (chatType == "group" || chatType == "supergroup") {
                        if (chatId !in games.keys) {
                            val game = Game(chatId, update.message!!.from!!.id)
                            games[chatId] = game
                            bot.sendMessage(chatId = chatId, text = """
                            New game created! Each player has to /join the game.
                            The initiator of this game (or the admin) can /join too and type /startgame when everyone has joined the game!
                        """.trimIndent())
                        } else {
                            bot.sendMessage(chatId = chatId, text = "A game is currently running. Type /cancelgame to end it.")
                        }
                    } else {
                        bot.sendMessage(chatId = chatId, text = "Add me to a group, lol (type /newgame in a group)")
                    }
                }
                command("cancelgame") { bot, update ->
                    val chatId = update.message!!.chat.id
                    if (chatId in games.keys) {
                        val game = games[chatId]!!
                        val userId = update.message!!.from!!.id
                        val status = bot.getChatMember(chatId, userId).first?.body()?.result?.status
                        if (status == "administrator" || status == "creator" || userId == game.initiatorId) {
                            endGame(bot, game, 99)
                        }
                    } else {
                        bot.sendMessage(chatId = chatId, text = "There is no game in this chat. Create a new game with /newgame")
                    }
                }
                command("startgame") { bot, update ->
                    val chatId = update.message!!.chat.id

                    if (games.containsKey(chatId)) {
                        val userId = update.message!!.from!!.id
                        val userResponse = bot.getChatMember(chatId, userId)
                        val responseStatus = userResponse.first!!.isSuccessful
                        if (responseStatus) {
                            val userStatus = userResponse.first!!.body()!!.result!!.status
                            val game = games[chatId]
                            if (game?.board != null) {
                                bot.sendMessage(chatId, "A game is already on!!!")
                            } else {
                                if (userStatus == "administrator" || userStatus == "creator" || userId == game!!.initiatorId) {
                                    if (game!!.playerList.size >= 5 && game.playerList.size <= 10) {
                                        game.start()
                                    }
                                }
                            }
                        }
                    }
                }
                command("join") { bot, update ->
                    val chatId = update.message!!.chat.id
                    val chatName = update.message!!.chat.title
                    val userId = update.message!!.from!!.id
                    if (chatId in games.keys) {
                        val userName = update.message!!.from!!.firstName
                        val game = games[chatId]
                        if (game!!.playerList.size < 10) {
                            if (!game.playerList.containsKey(userId)) {
                                val player = Player(name = userName, userId = userId)
                                if (bot.sendMessage(userId, "You joined a game in $chatName!").second != null) {
                                    bot.sendMessage(chatId, "I can't send you private messages, $userName. Go to @SecretHitlerKotlinBot")
                                } else {
                                    game.addPlayer(userId, player)
                                }
                            } else {
                                bot.sendMessage(userId, "You already joined a game in $chatName, $userName!")
                            }
                        } else {
                            bot.sendMessage(chatId, "You have reached the maximum amount of players. Please start the game with /startgame!")
                        }
                    } else {
                        bot.sendMessage(chatId, "There is no game in this chat! Type /newgame to create one.")
                    }
                }
                command("symbols") { bot, update ->
                    val chatId = update.message!!.chat.id
                    val builder = StringBuilder("The following symbols can appear on the board:")
                    for (symbol in Symbols.values()) {
                        builder.append('\n').append(symbol.getString()).append(symbol.description)
                    }
                    bot.sendMessage(chatId = chatId, text = builder.toString())
                }
                command("cxc") { bot, update ->
                    val chatId = update.message!!.chat.id
                    bot.sendMessage(chatId = chatId, text = "Саня хуй соси")
                }
            }
        }
        bot.startPolling()
    }

    fun endGame(bot : Bot, game : Game, endCode : Int) {
        // !!!!!!! END CODES !!!!!!!
        /*
        -2  fascists win by electing Hitler as chancellor
        -1  fascists win with 6 fascist policies
        0   not ended
        1   liberals win with 5 liberal policies
        2   liberals win by killing Hitler
        99  game cancelled
        */

    }
}