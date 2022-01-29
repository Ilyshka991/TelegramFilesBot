package com.pechuro.guitarbot.app

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.pechuro.guitarbot.domain.BotMessage
import com.pechuro.guitarbot.domain.BotMessageProvider
import com.pechuro.guitarbot.domain.BotMessageInfo
import com.pechuro.guitarbot.ext.getStringFromResources
import kotlin.reflect.KClass

class TelegramBot(private val messageProvider: BotMessageProvider) {

    private val botInstance = bot {
        token = Configuration.Telegram.TOKEN
        timeout = Configuration.Telegram.TIMEOUT_SEC
        logLevel = Configuration.Telegram.LOG_LEVEL
        dispatch {
            setupStartCommand()
            setupMainActionListener()
            setupMessageCallback()
        }
    }

    private val Message.chatId: ChatId
        get() = ChatId.fromId(chat.id)

    fun start() {
        botInstance.startPolling()
    }

    fun stop() {
        botInstance.stopPolling()
    }

    private fun Dispatcher.setupStartCommand() = command("start") {
        val chatId = message.chatId
        bot.sendWelcomeMessage(chatId)
        bot.sendHelpInfo(chatId)
        bot.sendBotMessage(chatId)
    }

    private fun Dispatcher.setupMainActionListener() = message(Filter.Text) {
        val chatId = message.chatId
        when (message.text) {
            getStringFromResources("action.showMainScreen") -> bot.sendBotMessage(chatId)
            getStringFromResources("action.updateInfo") -> bot.updateInfo(chatId)
            getStringFromResources("action.showHelp") -> bot.sendHelpInfo(chatId)
            else -> bot.findInfo(chatId, message.text)
        }
    }

    private fun Dispatcher.setupMessageCallback() = callbackQuery {
        val message = this.callbackQuery.message ?: return@callbackQuery
        val messageInfo = BotMessageInfo.deserialize(callbackQuery.data) ?: return@callbackQuery
        val botMsg = messageProvider.get(messageInfo) ?: return@callbackQuery
        bot.editMessageText(
            chatId = message.chatId,
            messageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
            text = botMsg.text,
            disableWebPagePreview = false,
            replyMarkup = botMsg.mapToReplyKeyboard(
                chatId = message.chatId,
                messageTypeClass = messageInfo::class
            )
        )
    }

    private fun Bot.sendWelcomeMessage(chatId: ChatId) {
        val buttons = listOf(
            listOf(KeyboardButton(getStringFromResources("action.showMainScreen"))),
            listOf(
                KeyboardButton(getStringFromResources("action.updateInfo")),
                KeyboardButton(getStringFromResources("action.showHelp"))
            )
        )
        val replyMarkup = KeyboardReplyMarkup(
            keyboard = buttons,
            resizeKeyboard = true,
            oneTimeKeyboard = false
        )
        sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.welcome"),
            disableWebPagePreview = false,
            disableNotification = true,
            replyMarkup = replyMarkup
        )
    }

    private fun Bot.sendBotMessage(
        chatId: ChatId,
        message: BotMessage.Content = messageProvider.rootMessage,
        messageTypeClass: KClass<out BotMessageInfo> = BotMessageInfo.Normal::class
    ) {
        sendMessage(
            chatId = chatId,
            text = message.text,
            parseMode = ParseMode.MARKDOWN_V2,
            disableNotification = true,
            disableWebPagePreview = false,
            replyMarkup = message.mapToReplyKeyboard(
                chatId = chatId,
                messageTypeClass = messageTypeClass
            )
        )
    }

    private fun Bot.sendHelpInfo(chatId: ChatId) {
        sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.helpInfo"),
            parseMode = ParseMode.MARKDOWN_V2,
            disableNotification = true,
            disableWebPagePreview = false
        )
    }

    private fun Bot.sendInfoUpdated(chatId: ChatId) {
        sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.infoUpdated"),
            disableNotification = true,
            disableWebPagePreview = false
        )
    }

    private fun Bot.updateInfo(chatId: ChatId) {
        messageProvider.sync()
        sendInfoUpdated(chatId)
        sendBotMessage(chatId)
    }

    private fun Bot.findInfo(chatId: ChatId, message: String?) {
        val predicate = message?.takeIf { it.isNotEmpty() } ?: return
        val chatIdHandle = (chatId as? ChatId.Id)?.id ?: return
        val msg = messageProvider.search(chatIdHandle, predicate)
        sendBotMessage(
            chatId = chatId,
            message = msg,
            messageTypeClass = BotMessageInfo.Search::class
        )
    }

    private fun BotMessage.Content.mapToReplyKeyboard(
        chatId: ChatId,
        messageTypeClass: KClass<out BotMessageInfo>
    ): ReplyMarkup {
        val buttons = nodes.mapNotNull {
            val (text, nextMsgId) = when (it) {
                is BotMessage.Content -> it.label to it.id
                is BotMessage.Back -> it.label to (it.parent?.id ?: 0)
            }
            val messageType = when (messageTypeClass) {
                BotMessageInfo.Search::class -> BotMessageInfo.Search(nextMsgId, (chatId as? ChatId.Id)?.id ?: 0)
                BotMessageInfo.Normal::class -> BotMessageInfo.Normal(nextMsgId)
                else -> return@mapNotNull null
            }
            InlineKeyboardButton.CallbackData(
                text = text,
                callbackData = messageType.serialize(),
            )
        }.splitToRows()
        return InlineKeyboardMarkup.create(buttons)
    }

    private fun List<InlineKeyboardButton>.splitToRows(): List<List<InlineKeyboardButton>> {
        val sortedNavigationButtons = sortNavigationButtons()
        val navigationButtons = sortedNavigationButtons.getNavigationButtons()
        return sortedNavigationButtons
            .drop(navigationButtons.size)
            .sortedBy { it.text }
            .chunked(2)
            .plus(listOf(navigationButtons))
    }

    private fun List<InlineKeyboardButton>.sortNavigationButtons() = sortedBy {
        when (it.text) {
            getStringFromResources("action.previousPage") -> 1
            getStringFromResources("action.nextPage") -> 2
            getStringFromResources("action.back") -> 3
            else -> 4
        }
    }

    private fun List<InlineKeyboardButton>.getNavigationButtons() = takeWhile {
        it.text == getStringFromResources("action.previousPage") ||
                it.text == getStringFromResources("action.nextPage") ||
                it.text == getStringFromResources("action.back")
    }
}