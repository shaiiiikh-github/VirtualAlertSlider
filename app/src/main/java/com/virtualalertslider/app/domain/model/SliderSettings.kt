package com.virtualalertslider.app.domain.model

data class SliderSettings(
    val defaultMode: AlertMode = AlertMode.RING,
    val hapticFeedbackEnabled: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val isNotificationPolicyAccessGranted: Boolean = false,
    val bypassStuckSlider: Boolean = true
)
