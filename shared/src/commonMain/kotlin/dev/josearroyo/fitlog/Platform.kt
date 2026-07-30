package dev.josearroyo.fitlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform