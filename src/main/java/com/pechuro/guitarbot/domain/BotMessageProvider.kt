package com.pechuro.guitarbot.domain

interface BotMessageProvider {

    val rootMessage: BotMessage.Content

    fun search(chatId: Long, predicate: String): BotMessage.Content

    fun get(messageInfo: BotMessageInfo): BotMessage.Content?

    fun sync()
}