package com.pechuro.guitarbot.domain

import com.pechuro.guitarbot.ext.getStringFromResources

sealed class BotMessage {

    abstract val id: Long
    abstract val parent: BotMessage?
    abstract val label: String


    data class Content(
        override val id: Long,
        override val parent: BotMessage?,
        override val label: String,
        var text: String,
        var nodes: List<BotMessage>
    ) : BotMessage()

    data class Back(
        override val parent: BotMessage?,
        override val label: String = getStringFromResources("action.back")
    ) : BotMessage() {

        override val id = -1L
    }
}