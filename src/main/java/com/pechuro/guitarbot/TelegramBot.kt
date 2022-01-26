package com.pechuro.guitarbot

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
import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.data.MessageHolder
import com.pechuro.guitarbot.domain.BotMessage
import com.pechuro.guitarbot.domain.BotMessageType
import com.pechuro.guitarbot.ext.getStringFromResources
import kotlin.reflect.KClass

private val Long.chatId: ChatId
    get() = ChatId.fromId(this)

class TelegramBot(private val messageHolder: MessageHolder) {

    val botInstance = bot {
        token = Configuration.Telegram.TOKEN
        timeout = Configuration.Telegram.TIMEOUT_SEC
        logLevel = Configuration.Telegram.LOG_LEVEL
        dispatch {
            setupStartCommand()
            setupMainActionListener()
            setupCallback()
        }
    }

    private fun Dispatcher.setupStartCommand() = command("start") {
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
        val chatId = message.chat.id.chatId
        bot.sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.welcome"),
            disableWebPagePreview = false,
            disableNotification = true,
            replyMarkup = replyMarkup
        )
        bot.sendHelpInfo(chatId)
        bot.sendStartInfo(chatId)
    }

    private fun Dispatcher.setupMainActionListener() = message(Filter.Text) {
        val chatId = message.chat.id.chatId
        when (message.text) {
            getStringFromResources("action.showMainScreen") -> bot.sendStartInfo(chatId)
            getStringFromResources("action.updateInfo") -> bot.updateInfo(chatId)
            getStringFromResources("action.showHelp") -> bot.sendHelpInfo(chatId)
            else -> bot.findInfo(chatId, message.text)
        }
    }

    private fun Dispatcher.setupCallback() = callbackQuery {
        val message = this.callbackQuery.message ?: return@callbackQuery
        val messageType = BotMessageType.deserialize(callbackQuery.data) ?: return@callbackQuery
        val botMsg = messageHolder.findMessage(messageType) as? BotMessage.Content ?: return@callbackQuery
        bot.editMessageText(
            chatId = message.chat.id.chatId,
            messageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
            text = botMsg.text,
            disableWebPagePreview = false,
            replyMarkup = botMsg.mapToReplyMarkup(
                chatIdHandle = message.chat.id,
                messageClazz = messageType::class
            )
        )
    }

    private fun Bot.sendStartInfo(chatId: ChatId) {
        sendMessage(
            chatId = chatId,
            text = messageHolder.rootMessage.text,
            parseMode = ParseMode.MARKDOWN_V2,
            disableNotification = true,
            disableWebPagePreview = false,
            replyMarkup = messageHolder.rootMessage.mapToReplyMarkup(
                chatIdHandle = (chatId as? ChatId.Id)?.id ?: 0,
                messageClazz = BotMessageType.Normal::class
            )
        )
    }

    private fun Bot.sendHelpInfo(chatId: ChatId) {
        sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.helpInfo"),
            parseMode = ParseMode.MARKDOWN_V2,
            disableNotification = true
        )
    }

    private fun Bot.updateInfo(chatId: ChatId) {
        messageHolder.loadMessages()
        sendMessage(
            chatId = chatId,
            text = getStringFromResources("message.infoUpdated"),
            disableNotification = true,
        )
        sendStartInfo(chatId)
    }

    private fun Bot.findInfo(chatId: ChatId, message: String?) {
        val predicate = message?.takeIf { it.isNotEmpty() } ?: return
        val chatIdHandle = (chatId as? ChatId.Id)?.id ?: return
        val msg = messageHolder.startSearch(predicate, chatIdHandle)
        sendMessage(
            chatId = chatId,
            text = msg.text,
            parseMode = ParseMode.MARKDOWN_V2,
            disableNotification = true,
            disableWebPagePreview = false,
            replyMarkup = msg.mapToReplyMarkup(
                chatIdHandle = (chatId as? ChatId.Id)?.id ?: 0,
                messageClazz = BotMessageType.Search::class
            )
        )
    }

    private fun BotMessage.Content.mapToReplyMarkup(
        chatIdHandle: Long,
        messageClazz: KClass<out BotMessageType>
    ): ReplyMarkup {
        val buttons = nodes.mapNotNull {
            val (text, nextMsgId) = when (it) {
                is BotMessage.Content -> it.label to it.id
                is BotMessage.Back -> it.label to (it.parent?.id ?: 0)
            }
            val messageType = when (messageClazz) {
                BotMessageType.Search::class -> BotMessageType.Search(nextMsgId, chatIdHandle)
                BotMessageType.Normal::class -> BotMessageType.Normal(nextMsgId)
                else -> return@mapNotNull null
            }
            InlineKeyboardButton.CallbackData(
                text = text,
                callbackData = messageType.serialize(),
            )
        }
        return InlineKeyboardMarkup.createSingleRowKeyboard(buttons)
    }
}