package com.anurag.eduai.notificationdemo.pipeline

object Messages {
    val fiveMinMessages = listOf(
        "It's been 5 minutes — come back and pick up where you left off!",
        "5 minutes gone quiet. Ready to jump back in?",
        "We noticed you stepped away 5 minutes ago. Still there?"
    )

    val tenMinMessages = listOf(
        "10 minutes and counting — don't lose your momentum!",
        "It's been 10 minutes. Your session is waiting for you.",
        "10-minute check-in: tap back in whenever you're ready."
    )

    // "X" is a literal placeholder — replaced with the current streak count
    // wherever it appears in the string.
    val streakMessages = listOf(
        "🔥 X-streak! You're on fire, keep going!",
        "Amazing! X in a row — don't break the chain now."
    )

    val reminderMessages = listOf(
        "Time for your daily study session — let's go!",
        "Start today's session now."
    )
}
