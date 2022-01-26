package com.pechuro.guitarbot.app

import com.pechuro.guitarbot.TelegramBot
import com.pechuro.guitarbot.data.GoogleDriveRepository
import com.pechuro.guitarbot.data.MessageHolder
import kotlinx.coroutines.*

private const val UPDATE_MESSAGES_DELAY_MS = 1 * 24 * 60 * 60 * 1000L

suspend fun main() = withContext(Dispatchers.IO) {
    println("AAAAAAA")
    val dataRepository = GoogleDriveRepository()
    val messageHolder = MessageHolder(repository = dataRepository).apply {
        loadMessages()
    }
    loadMessagesRoutine(messageHolder)
    TelegramBot(messageHolder).botInstance.startPolling()
}

private fun CoroutineScope.loadMessagesRoutine(messageHolder: MessageHolder) = launch {
    while (isActive) {
        delay(UPDATE_MESSAGES_DELAY_MS)
        messageHolder.loadMessages()
    }
}
