package com.pechuro.guitarbot.app

import com.github.kotlintelegrambot.logging.LogLevel
import com.pechuro.BuildConfig

object Configuration {

    object App {
        val APPLICATION_NAME: String = System.getenv("application.name") ?: BuildConfig.APP_NAME
        const val MAX_FILES_PER_PAGE = 6
    }

    object Google {
        val API_KEY: String = (System.getenv("google.api_key_json") ?: BuildConfig.GOOGLE_API_KEY_JSON).also { println("AAAA $it") }
        val ROOT_FILE_PATH: String = System.getenv("google.root_file_path") ?: BuildConfig.GOOGLE_ROOT_FILE_PATH
    }

    object Telegram {
        val TOKEN: String = System.getenv("telegram.bot_token") ?: BuildConfig.TELEGRAM_BOT_TOKEN
        const val TIMEOUT_SEC = 30
        val LOG_LEVEL = LogLevel.Network.Body
    }
}
