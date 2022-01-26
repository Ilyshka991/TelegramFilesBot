package com.pechuro.guitarbot.app

import com.github.kotlintelegrambot.logging.LogLevel
import com.pechuro.BuildConfig

object Configuration {

    object App {
        const val APPLICATION_NAME: String = BuildConfig.APP_NAME
        const val MAX_FILES_PER_PAGE = 6
    }

    object Google {
         val API_KEY: String = BuildConfig.GOOGLE_API_KEY_JSON.also {
            println("AAAAA $it")
        }
        const val ROOT_FILE_PATH: String = BuildConfig.GOOGLE_ROOT_FILE_PATH
    }

    object Telegram {
        const val TOKEN: String = BuildConfig.TELEGRAM_BOT_TOKEN
        const val TIMEOUT_SEC = 30
        val LOG_LEVEL = LogLevel.Network.Body
    }
}
