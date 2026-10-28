package com.virtualalertslider.app.domain.model

enum class ThemePreference(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemePreference {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}
