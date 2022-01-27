package com.pechuro.guitarbot

import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.app.TelegramBot
import com.pechuro.guitarbot.data.impl.GoogleDriveRepository
import com.pechuro.guitarbot.data.impl.DefaultMessageProvider
import kotlinx.coroutines.*

suspend fun main() = withContext(Dispatchers.IO) {
    val dataRepository = GoogleDriveRepository()
    val messageProvider = DefaultMessageProvider(repository = dataRepository)
    startPeriodicMessageSyncing(messageProvider)
    TelegramBot(messageProvider).start()
}

private fun CoroutineScope.startPeriodicMessageSyncing(messageProvider: DefaultMessageProvider) = launch {
    while (isActive) {
        delay(Configuration.App.SYNC_MESSAGES_DELAY_MS)
        messageProvider.sync()
    }
}
