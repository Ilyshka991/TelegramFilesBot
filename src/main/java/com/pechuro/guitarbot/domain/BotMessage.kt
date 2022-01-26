package com.pechuro.guitarbot.domain

import com.pechuro.guitarbot.ext.getStringFromResources

sealed class BotMessage {

    abstract val parent: Content?
    abstract val label: String

    data class Content(
        val id: Long,
        override val parent: Content?,
        override val label: String,
        var text: String,
        var nodes: List<BotMessage>
    ) : BotMessage()

    data class Back(
        override val parent: Content?,
        override val label: String = getStringFromResources("action.back")
    ) : BotMessage()
}