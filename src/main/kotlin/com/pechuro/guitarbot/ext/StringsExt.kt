package com.pechuro.guitarbot.ext

import java.util.*

private val stringsBundle = ResourceBundle.getBundle("strings")

fun getStringFromResources(key: String): String = stringsBundle.getString(key)