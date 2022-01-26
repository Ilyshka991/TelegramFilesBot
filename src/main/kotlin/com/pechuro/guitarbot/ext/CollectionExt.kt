package com.pechuro.guitarbot.ext

fun <T> List<T>.plusNotNull(element: T?): List<T> = if (element != null) {
    this.plus(element)
} else {
    this
}