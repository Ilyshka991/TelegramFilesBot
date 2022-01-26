package com.pechuro.guitarbot.domain

sealed class BotMessageType {

    companion object {

        private const val NORMAL_TYPE_ID = 1
        private const val SEARCH_TYPE_ID = 2
        private const val DELIMITER = '_'

        fun deserialize(value: String): BotMessageType? {
            val parts = value.split(DELIMITER)
            return when (parts.first().toInt()) {
                NORMAL_TYPE_ID -> Normal(parts.last().toLong())
                SEARCH_TYPE_ID -> Search(
                    parts[1].toLong(),
                    parts[2].toLong()
                )
                else -> null
            }
        }
    }

    abstract val id: Long

    data class Normal(
        override val id: Long
    ) : BotMessageType()

    data class Search(
        override val id: Long,
        val chatId: Long
    ) : BotMessageType()

    fun serialize() = buildString {
        val typeId = when (this@BotMessageType) {
            is Normal -> NORMAL_TYPE_ID
            is Search -> SEARCH_TYPE_ID
        }
        append(typeId)
        append(DELIMITER)
        append(id)
        if (this@BotMessageType is Search) {
            append(DELIMITER)
            append(chatId)
        }
    }
}
