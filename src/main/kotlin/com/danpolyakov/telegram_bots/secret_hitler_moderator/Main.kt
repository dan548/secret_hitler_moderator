package com.danpolyakov.telegram_bots.secret_hitler_moderator

import me.ivmg.telegram.Bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.entities.InlineKeyboardButton
import com.natpryce.konfig.*
import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatcher.callbackQuery
import me.ivmg.telegram.dispatcher.text
import me.ivmg.telegram.entities.CallbackQuery
import me.ivmg.telegram.entities.InlineKeyboardMarkup
import kotlin.random.Random
import kotlin.random.nextInt

fun main() {

    val games = mutableMapOf<Long, Game>()

    val message = """
            "Тайный Гитлер - игра социальных вычетов для 5-10 человек о поиске и остановке Тайного Гитлера. Большинство игроков - либералы. Если они могут научиться доверять друг другу, у них достаточно голосов, чтобы контролировать стол и выигрывать игру. Но некоторые игроки - фашисты. Они скажут все, что нужно, чтобы быть избранными, принять свою повестку дня и обвинить других в последствиях. Либералы должны работать вместе, чтобы обнаружить правду, прежде чем фашисты установят своего хладнокровного лидера и выиграют игру ".
            |- Официальное описание Секретного Гитлера
            
            |Добавьте меня в группу и введите /newgame, чтобы создать игру!""".trimMargin()

    val commands = """Доступны следующие команды:
            |/help - Предоставление информации о доступных командах
            |/start - Показать сообщение и список команд
            |/newgame - Create a new game
            |/rules - Rules
            |/cancelgame - Cancel current game
            |/startgame - Start the game
            |/join - Join the game
            |/symbols - board symbols""".trimMargin()

    val key = Key("bot.api-key", stringType)
    val config = ConfigurationProperties.fromResource("app.properties")

    val patterns : MutableList<Pair<Regex, String>> = ArrayList()

    patterns.add(Pair(Regex("""(\d+)_chan_(.+)"""), "chancellor"))
    patterns.add(Pair(Regex("""(\d+)_insp_(.+)"""), "inspect"))
    patterns.add(Pair(Regex("""(\d+)_choo_(.+)"""), "choose"))
    patterns.add(Pair(Regex("""(\d+)_kill_(.+)"""), "kill"))
    patterns.add(Pair(Regex("""(\d+)_(yesveto|noveto)"""), "veto"))
    patterns.add(Pair(Regex("""(\d+)_(liberal|veto|fascist)"""), "enact"))
    patterns.add(Pair(Regex("""(\d+)_(Ja|Nein)"""), "voting"))

    val bot = bot {
        token = config[key]
        dispatch {
            callbackQuery { bot, update ->
                val query = update.callbackQuery?.data ?: ""
                val currentPair = patterns.find { pair -> pair.first.matches(query) }
                when (currentPair?.second){
                    "chancellor" -> update.callbackQuery?.let {
                        val (voterId, gameId, chancellorId) = parseCallback(it, currentPair, query)
                        val game = games[gameId?.toLong()]!!
                        val chancellor = game.getPlayerById(chancellorId?.toLong())
                        val voter = game.getPlayerById(voterId?.toLong())
                        game.state().nominatedChancellor = chancellor
                        bot.editMessageText(voterId?.toLong(), update.callbackQuery?.message?.messageId,
                            text = "You nominated ${chancellor!!.name} as chancellor!")
                        bot.sendMessage(gameId?.toLong() ?: 0,
                            "President ${voter!!.name} nominated ${chancellor.name} " +
                                    "as chancellor. Please vote now!")
                        vote(bot, game)
                    }
                    "inspect" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it, currentPair, query)
                        val inspectorId = parsed[0]?.toLong()
                        val gameId = parsed[1]?.toLong()
                        val chosenId = parsed[2]?.toLong()
                        val game = games[gameId!!]!!
                        val chosen = game.getPlayerById(chosenId)

                        bot.editMessageText(inspectorId, update.callbackQuery?.message?.messageId,
                            text = "The party membership of ${chosen?.name} is ${chosen?.party}")
                        bot.sendMessage(gameId, "President ${game.state().president?.name} inspected ${chosen?.name}.")
                        startPreRound(bot, game)
                    }
                    "choose" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it,  currentPair, query)
                        val fromId = parsed[0]?.toLong()
                        val gameId = parsed[1]?.toLong()
                        val sEId = parsed[2]?.toLong()
                        val game = games[gameId]!!
                        val sE = game.getPlayerById(sEId)

                        game.state().chosenPresident = sE
                        bot.editMessageText(fromId, update.callbackQuery?.message?.messageId,
                            text = "You chose $sE as the next president!")
                        bot.sendMessage(gameId!!,
                            "President ${game.state().president?.name} chose ${sE?.name} as the next president.")
                        startPreRound(bot, game)
                    }
                    "kill" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it,  currentPair, query)
                        val whoId = parsed[0]?.toLong()
                        val whereId = parsed[1]!!.toLong()
                        val whomId = parsed[2]?.toLong()
                        val game = games[whereId]!!
                        val dead = game.getPlayerById(whomId)

                        game.killPlayer(whomId)

                        bot.editMessageText(whoId, update.callbackQuery?.message?.messageId,
                            text = "You killed ${dead?.name}!")

                        if (dead?.role == "Hitler") {
                            bot.sendMessage(whereId, "President ${game.state().president?.name} killed ${dead.name}.")
                            endGame(bot, game, 2)
                        } else {
                            bot.sendMessage(whereId,
                                "President ${game.state().president?.name} killed ${dead?.name} who was not Hitler. ${dead?.name}, you are dead now and are not allowed to talk anymore!")
                            bot.sendMessage(whereId, game.board.print())
                            startPreRound(bot, game)
                        }
                    }
                    "veto" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it,  currentPair, query)
                        val fromId = parsed[0]?.toLong()
                        val gameId = parsed[1]!!.toLong()
                        val answer = parsed[2]!!
                        val game = games[gameId]

                        if (answer == "yesveto") {
                            bot.editMessageText(fromId, update.callbackQuery?.message?.messageId,
                                text = "You accepted the Veto!")
                            bot.sendMessage(gameId,
                                "President ${game!!.state().president!!.name} accepted Chancellor ${game.state().chancellor!!.name}'s Veto. " +
                                        "No policy was enacted but this counts as a failed election.")
                            game.state().drawnPolicies.clear()
                            game.state().failedVotes++
                            if (game.state().failedVotes == 3) {
                                doAnarchy(bot, game)
                            } else {
                                bot.sendMessage(gameId, game.board.print())
                                startPreRound(bot, game)
                            }
                        } else {
                            if (answer == "noveto") {
                                game!!.state().vetoRefused = true
                                bot.editMessageText(fromId, update.callbackQuery?.message?.messageId,
                                    text = "You refused the Veto!")
                                bot.sendMessage(gameId,
                                    "President ${game.state().president?.name} refused Chancellor ${game.state().chancellor?.name}'s Veto. The Chancellor now has to choose a policy!")
                                passTwoPolicies(bot, game)
                            }
                        }
                    }
                    "enact" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it,  currentPair, query)
                        val fromId = parsed[0]?.toLong()
                        val gameId = parsed[1]?.toLong()
                        val policyType = parsed[2]!!
                        val game = games[gameId]!!

                        if (game.state().drawnPolicies.size == 3) {
                            bot.editMessageText(fromId, update.callbackQuery?.message?.messageId,
                                text = "The policy $policyType will be discarded!")
                            game.state().drawnPolicies.removeAt(game.state().drawnPolicies.indexOf(policyType))
                            passTwoPolicies(bot, game)
                        } else {
                            if (game.state().drawnPolicies.size == 2) {
                                if (policyType == "veto") {
                                    bot.editMessageText(fromId, update.callbackQuery?.message?.messageId, text = "You suggested a Veto to President ${game.state().president?.name}")
                                    bot.sendMessage(gameId!!,
                                        "Chancellor ${game.state().chancellor?.name} suggested a Veto to President ${game.state().president?.name}.")

                                    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()

                                    btns.add(listOf(InlineKeyboardButton("Veto! (accept suggestion)", callbackData = "${gameId}_yesveto")))
                                    btns.add(listOf(InlineKeyboardButton("No Veto! (refuse suggestion)", callbackData = "${gameId}_noveto")))

                                    val vetoMarkup = InlineKeyboardMarkup(btns)
                                    bot.sendMessage(game.state().president!!.userId,
                                        "Chancellor ${game.state().chancellor!!.name} suggested a Veto to you. Do you want to veto (discard) these cards?",
                                        replyMarkup=vetoMarkup)
                                } else {
                                    bot.editMessageText(fromId, update.callbackQuery?.message?.messageId,
                                        text = "The policy $policyType will be enacted!")
                                    game.state().drawnPolicies.clear()
                                    enactPolicy(bot, game, policyType)
                                }
                            }
                        }

                    }
                    "voting" -> update.callbackQuery?.let {
                        val parsed = parseCallback(it,  currentPair, query)
                        val userId = parsed[0]?.toLong()
                        val gameId = parsed[1]?.toLong()
                        val answer = parsed[2] ?: ""
                        val game = games[gameId]
                        val votes = game?.state()?.lastVotes
                        if (votes != null && userId != null) {
                            if (userId in votes.keys) {
                                if (votes[userId] != answer) {
                                    votes[userId] = answer
                                    bot.sendMessage(userId, "You've changed your mind and voted $answer")
                                }
                            } else {
                                votes[userId] = answer
                                bot.sendMessage(userId, "Thank you for your vote! You voted $answer to the " +
                                        "president ${game.state().nominatedPresident?.name} and " +
                                        "the chancellor ${game.state().nominatedChancellor?.name}")
                            }
                        }
                        if (game != null && (game.getAlive().size == game.state().lastVotes.keys.size)) {
                            countVotes(bot, game)
                        }
                    }
                }
            }
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
                        games.remove(chatId)
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
                        val userStatus = userResponse.first?.body()?.result?.status
                        val game = games[chatId]!!
                        if (game.isStarted()) {
                            bot.sendMessage(chatId, "A game is already on!!!")
                        } else {
                            if (userStatus == "administrator" || userStatus == "creator" || userId == game.initiatorId) {
                                if (game.getAll().size in 5..10) {
                                    game.start()
                                    bot.sendMessage(chatId, "The game has started.")
                                    game.informPlayers(bot)
                                    startPreRound(bot, game)
                                } else {
                                    bot.sendMessage(chatId, "Bad player number.")
                                }
                            } else {
                                bot.sendMessage(chatId,
                                    "${game.getPlayerById(userId)?.name}, you have no rights to start the game.")
                            }
                        }
                    }
                } else {
                    bot.sendMessage(chatId, "There are no games in this chat.")
                }
            }
            command("join") { bot, update ->
                val chatId = update.message!!.chat.id
                val chatName = update.message!!.chat.title
                val userId = update.message!!.from!!.id
                if (chatId in games.keys) {
                    val userName = update.message!!.from!!.firstName
                    val game = games[chatId]!!
                    if (game.getAll().size < 10) {
                        if (game.getAll().firstOrNull{player -> player.userId == userId} == null) {
                            val player = Player(name = userName, userId = userId)
                            val response = bot.sendMessage(userId, "You joined a game in $chatName!").first
                            if (!response!!.isSuccessful) {
                                bot.sendMessage(chatId, "I can't send you private messages, $userName. Go to @SecretHitlerKotlinBot")
                            } else {
                                game.addPlayer(player)
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
                    builder.append('\n').append(symbol).append(symbol.description)
                }
                bot.sendMessage(chatId = chatId, text = builder.toString())
            }
            command("cxc") { bot, update ->
                val chatId = update.message!!.chat.id
                val list = listOf("Саня", "Слава")
                bot.sendMessage(chatId = chatId, text = "${list[Random.nextInt(0..1)]} хуй соси")
            }
            text() { bot, update ->
                val chatId = update.message!!.chat.id
                val authorId = update.message!!.from!!.id

                if (games.containsKey(chatId) && games[chatId]!!.isStarted() && !games[chatId]!!.getAliveIds().contains(authorId)) {
                    bot.deleteMessage(chatId, update.message!!.messageId)
                }
            }
        }
    }
    bot.startPolling()
}

fun startPreRound(bot : Bot, game : Game) {
    if (game.state().endCode == 0) {
        Thread.sleep(3000)
        if (game.state().chosenPresident == null) {
            incrPlayerCounter(game)
        }
        startRound(bot, game)
    }
}

private fun countVotes(bot : Bot, game : Game) {
    val votingText = StringBuilder()
    var votingSuccess = false

    for (player in game.getAlive()) {
        votingText.append("${player.name} voted ${game.state().lastVotes[player.userId]}!\n")
    }

    if (game.state().lastVotes.count { vote: Map.Entry<Long, String> -> vote.value == "Ja" } > game.getAlive().size / 2) {
        votingText.append("Hail President ${game.state().nominatedPresident?.name}! " +
                "Hail Chancellor ${game.state().nominatedChancellor?.name}!")
        votingSuccess = true
        game.state().chancellor = game.state().nominatedChancellor
        game.state().president = game.state().nominatedPresident
        game.state().nominatedChancellor = null
        game.state().nominatedPresident = null
        bot.sendMessage(game.chatId, votingText.toString())
        votingAftermath(bot, game, votingSuccess)
    } else {
        votingText.append("Voting failed!")
        game.state().nominatedChancellor = null
        game.state().nominatedPresident = null
        game.state().failedVotes++
        if (game.state().failedVotes == 3) {
            doAnarchy(bot, game)
        } else {
            bot.sendMessage(game.chatId, votingText.toString())
            votingAftermath(bot, game, votingSuccess)
        }
    }
}

private fun doAnarchy(bot : Bot, game : Game) {
    bot.sendMessage(game.chatId, game.board.print())
    bot.sendMessage(game.chatId, "ANARCHY!!")
    val topPolicy = game.board.getTopPolicy()
    game.state().lastVotes.clear()
    enactPolicy(bot, game, topPolicy, anarchy = true)
}

private fun votingAftermath(bot : Bot, game : Game, votingSuccess : Boolean) {
    game.state().lastVotes.clear()
    if (votingSuccess) {
        if (game.state().fascistTrack >= 3 && game.state().chancellor?.role == "Hitler") {
            game.state().endCode = -2
            endGame(bot, game, game.state().endCode)
        } else {
            drawPolicies(bot, game)
        }
    } else {
        bot.sendMessage(game.chatId, game.board.print())
        startPreRound(bot, game)
    }
}

private fun enactPolicy(bot : Bot, game : Game, policy : String, anarchy : Boolean = false) {
    game.board.enactPolicy(policy)

    if (policy == "liberal") {
        game.state().liberalTrack++
    } else {
        if (policy == "fascist") {
            game.state().fascistTrack++
        }
    }
    game.state().failedVotes = 0
    if (anarchy) {
        bot.sendMessage(game.chatId,
            "The top policy was enacted: $policy")
    } else {
        bot.sendMessage(game.chatId,
            "President ${game.state().president?.name} and Chancellor ${game.state().chancellor?.name}" +
                    " enacted a $policy policy!")
    }
    Thread.sleep(2000)
    bot.sendMessage(game.chatId, game.board.print())
    if (game.state().liberalTrack == 5) {
        game.state().endCode = 1
        endGame(bot, game, game.state().endCode)
    }
    if (game.state().fascistTrack == 6) {
        game.state().endCode = -1
        endGame(bot, game, game.state().endCode)
    }
    Thread.sleep(2000)
    if (!anarchy) {
        if (policy == "fascist") {
            when(game.board.fascistTrackActions[game.state().fascistTrack - 1]) {
                null -> if (game.state().fascistTrack < 6) startPreRound(bot, game)
                "policy" -> {
                    bot.sendMessage(game.chatId,
                        """Presidential Power enabled: Policy Peek ${Symbols.POLICY_PEEK}
                            |President ${game.state().president?.name} now knows the next three policies on the pile. The President may share (or lie about!) the results of their investigation at their discretion.""".trimMargin())
                    actionPolicy(bot, game)
                }
                "kill" -> {
                    bot.sendMessage(game.chatId,
                        """Presidential Power enabled: Execution ${Symbols.EXECUTE}
                            |President ${game.state().president?.name} has to kill one person. You can discuss the decision now but the President has the final say.""".trimMargin())
                    actionKill(bot, game)
                }
                "inspect" -> {
                    bot.sendMessage(game.chatId,
                        """Presidential Power enabled: Investigate Loyalty ${Symbols.INVEST}
                            |President ${game.state().president?.name} may see the party membership of one player. The President may share (or lie about!) the results of their investigation at their discretion.""".trimMargin())
                    actionInspect(bot, game)
                }
                "choose" -> {
                    bot.sendMessage(game.chatId,
                        """Presidential Power enabled: Special Election ${Symbols.SPECIAL_ELECTION}
                            |President ${game.state().president?.name} gets to choose the next presidential candidate. Afterwards the order resumes back to normal.""".trimMargin())
                    actionChoose(bot, game)
                }
            }
        } else {
            startPreRound(bot, game)
        }
    } else {
        startPreRound(bot, game)
    }
}

private fun passTwoPolicies(bot : Bot, game : Game) {
    val gameId = game.chatId
    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()
    for (policy in game.state().drawnPolicies) {
        btns.add(listOf(InlineKeyboardButton(policy,
            callbackData = "${gameId}_$policy")))
    }
    if (game.state().fascistTrack == 5 && !game.state().vetoRefused) {
        btns.add(listOf(
            InlineKeyboardButton("Veto",
            callbackData = "${gameId}_veto")))
        val choosePolicyMarkup = InlineKeyboardMarkup(btns)
        bot.sendMessage(gameId, "President ${game.state().president?.name} gave two policies to Chancellor ${game.state().chancellor?.name}.")
        bot.sendMessage(game.state().chancellor!!.userId,
            "President ${game.state().president?.name} gave you the following 2 policies. Which one do you want to enact? You can also use your Veto power.",
            replyMarkup = choosePolicyMarkup)
    } else {
        if (game.state().vetoRefused) {
            val choosePolicyMarkup = InlineKeyboardMarkup(btns)
            bot.sendMessage(game.state().chancellor!!.userId,
                "President ${game.state().president?.name} refused your Veto. Now you have to choose. Which one do you want to enact?",
                replyMarkup = choosePolicyMarkup)
        } else {
            if (game.state().fascistTrack < 5) {
                val choosePolicyMarkup = InlineKeyboardMarkup(btns)
                bot.sendMessage(game.state().chancellor!!.userId,
                    "President ${game.state().president?.name} gave you the following 2 policies. Which one do you want to enact?",
                    replyMarkup = choosePolicyMarkup)
            }
        }
    }
}

private fun drawPolicies(bot : Bot, game : Game) {
    val gameId = game.chatId
    game.state().vetoRefused = false
    shufflePolicyPile(bot, game)
    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()
    for (i in 0..2) {
        game.state().drawnPolicies.add(game.board.getTopPolicy())
    }
    for (policy in game.state().drawnPolicies) {
        btns.add(listOf(InlineKeyboardButton(policy, "${gameId}_$policy")))
    }
    val choosePolicyMarkup = InlineKeyboardMarkup(btns)
    bot.sendMessage(game.state().president!!.userId,
        "You drew the following 3 policies. Which one do you want to discard?",
        replyMarkup = choosePolicyMarkup)
}

private fun actionPolicy(bot : Bot, game : Game) {
    val topPolicies = StringBuilder()
    shufflePolicyPile(bot, game)
    for (i in 0..2) {
        topPolicies.append(game.board.getTopPolicy()).append("\n")
    }
    bot.sendMessage(game.state().president!!.userId,
        "The top three polices are (top most first):\n$topPolicies\nYou may lie about this.")
    startPreRound(bot, game)
}

private fun actionChoose(bot : Bot, game : Game) {
    val gameId = game.chatId
    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()

    for (player in game.getAlive()) {
        if (player.userId != game.state().president?.userId) {
            val name = player.name
            btns.add(listOf(
                InlineKeyboardButton(name,
                callbackData = "${gameId}_choo_${player.userId}")))
        }
    }

    val chooseMarkup = InlineKeyboardMarkup(btns)
    bot.sendMessage(game.state().president!!.userId, game.board.print())
    bot.sendMessage(game.state().president!!.userId,
        "You get to choose the next presidential candidate." +
                "Afterwards the order resumes back to normal. " +
                "Choose wisely!",
        replyMarkup = chooseMarkup)
}

private fun actionKill(bot : Bot, game : Game) {
    val gameId = game.chatId
    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()
    for (player in game.getAlive()) {
        if (player.userId != game.state().president?.userId) {
            val name = player.name
            btns.add(listOf(InlineKeyboardButton(name,
                callbackData = "${gameId}_kill_${player.userId}")))
        }
    }
    val killMarkup = InlineKeyboardMarkup(btns)
    bot.sendMessage(game.state().president!!.userId, game.board.print())
    bot.sendMessage(game.state().president!!.userId,
        "You have to kill one person. You can discuss your decision with" +
                "the others. Choose wisely!",
        replyMarkup = killMarkup)
}

private fun actionInspect(bot : Bot, game : Game) {
    val gameId = game.chatId
    val btns : MutableList<List<InlineKeyboardButton>> = ArrayList()

    for (player in game.getAlive()) {
        if (player.userId != game.state().president?.userId) {
            val name = player.name
            btns.add(listOf(
                InlineKeyboardButton(name,
                    callbackData = "${gameId}_insp_${player.userId}")))
        }
    }

    val inspectMarkup = InlineKeyboardMarkup(btns)
    bot.sendMessage(game.state().president!!.userId, game.board.print())
    bot.sendMessage(game.state().president!!.userId,
        "You may see the party membership of one player. " +
                "Which do you want to know? Choose wisely!",
        replyMarkup = inspectMarkup)
}

private fun shufflePolicyPile(bot : Bot, game : Game) {
    if (game.board.policiesLeft < 3) {
        game.board.shufflePile()
        bot.sendMessage(game.chatId, "The pile is now reshuffled!")
    }
}

private fun startRound(bot : Bot, game : Game) {
    if (game.state().chosenPresident == null) {
        game.state().nominatedPresident =
            game.getAlive()[game.state().playerCounter]
    } else {
        game.state().nominatedPresident = game.state().chosenPresident
        game.state().chosenPresident = null
    }
    val name = game.state().nominatedPresident?.name
    bot.sendMessage(game.chatId,
        "The next presidential candidate is $name.\n" +
                "$name, please nominate a Chancellor in our private chat!")
    chooseChancellor(bot, game)
}

private fun chooseChancellor(bot : Bot, game : Game) {
    val presId : Long? = game.state().president?.userId
    val chanId : Long? = game.state().chancellor?.userId
    val gameId = game.chatId
    val buttons : MutableList<InlineKeyboardButton> = ArrayList()
    val playerList = game.getAlive()

    playerList.forEach { player ->
        if (game.state().alive > 5) {
            if (player.userId != game.state().nominatedPresident!!.userId &&
                player.userId != presId && player.userId != chanId) {
                val name = player.name
                buttons.add(InlineKeyboardButton(name, callbackData = "${gameId}_chan_${player.userId}"))
            }
        } else {
            if (player.userId != game.state().nominatedPresident!!.userId && player.userId != chanId) {
                val name = player.name
                buttons.add(InlineKeyboardButton(name, callbackData = "${gameId}_chan_${player.userId}"))
            }
        }
    }

    val chancellorMarkup = InlineKeyboardMarkup(listOf(buttons))
    bot.sendMessage(game.state().nominatedPresident!!.userId, game.board.print())
    bot.sendMessage(game.state().nominatedPresident!!.userId, "Please nominate your chancellor!",
        replyMarkup = chancellorMarkup)
}

private fun incrPlayerCounter(game : Game) {
    if (game.state().playerCounter < game.board.playerCount - 1) {
        game.state().playerCounter++
    } else {
        game.state().playerCounter = 0
    }
}

private fun vote(bot : Bot, game : Game) {
    val buttons = listOf(listOf(InlineKeyboardButton("Ja", callbackData = "${game.chatId}_Ja"),
        InlineKeyboardButton("Nein", callbackData = "${game.chatId}_Nein")))
    val voteMarkup = InlineKeyboardMarkup(buttons)
    for (player in game.getAlive()) {
        if (player != game.state().nominatedPresident) {
            bot.sendMessage(player.userId, game.board.print())
        }
        bot.sendMessage(player.userId, "Do you want to elect President ${game.state().nominatedPresident!!.name} and" +
                " Chancellor ${game.state().nominatedChancellor!!.name}?", replyMarkup = voteMarkup)
    }
}

fun endGame(bot : Bot, game : Game, endCode : Int) {
    when (endCode) {
        -2 -> bot.sendMessage(game.chatId, "Game over! The fascists win by electing Hitler as Chancellor!\n\n${game.printRoles()}")
        -1 -> bot.sendMessage(game.chatId, "Game over! The fascists win by enacting 6 fascist policies!\n\n${game.printRoles()}")
        1 -> bot.sendMessage(game.chatId, "Game over! The liberals win by enacting 5 liberal policies!\n\n${game.printRoles()}")
        2 -> bot.sendMessage(game.chatId, "Game over! The liberals win by killing Hitler!\n\n${game.printRoles()}")
        99 -> {
            bot.sendMessage(game.chatId, "Game cancelled!\n\n${game.printRoles()}")
        }
    }
}

private fun parseCallback(it : CallbackQuery, pair : Pair<Regex, String>, query : String) : List<String?> {
    val fromId = it.message?.chat?.id?.toString()
    val groups = pair.first.find(query)?.groupValues
    val firstValue = groups?.get(0)
    val secondValue = groups?.get(1)
    return listOf(fromId, firstValue, secondValue)
}