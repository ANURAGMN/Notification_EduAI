package com.anurag.eduai.notificationdemo

/**
 * Category = the Android NotificationChannel boundary (§5 / §7 of NOTIFICATION_USE_CASES.md).
 * Each category owns a fallback icon + tint used for the small icon accent color
 * and the avatar-circle fallback background.
 */
enum class NotificationCategory(
    val channelId: String,
    val channelLabel: String,
    val dotColorRes: Int,
    val textColorRes: Int,
    val bgColorRes: Int,
    val highImportanceDefault: Boolean,
) {
    // Channel-level importance is coarser than the doc's per-type priority (§7) - Android's
    // NotificationChannel importance can't vary per-notification on API26+, only per-channel.
    // Streaks gets IMPORTANCE_HIGH as the closest approximation so Streak-at-risk demos as
    // heads-up; a production build might split urgent vs. praise into separate channels instead.
    STREAKS("streaks", "Streaks", R.color.warning_dot, R.color.warning_text, R.color.warning_bg, highImportanceDefault = true),
    QUESTS("quests", "Quests", R.color.success_dot, R.color.success_text, R.color.success_bg, highImportanceDefault = false),
    REMINDERS("reminders", "Reminders", R.color.accent_dot, R.color.accent_text, R.color.accent_bg, highImportanceDefault = false),
    AVATAR("avatar", "Avatar", R.color.pro_dot, R.color.pro_text, R.color.pro_bg, highImportanceDefault = false),
    LEAGUES_SOCIAL("leagues_social", "Leagues & social", R.color.accent_dot, R.color.accent_text, R.color.accent_bg, highImportanceDefault = false),
}

/** One entry per row in §1 / §2 / §4 of NOTIFICATION_USE_CASES.md. */
enum class NotificationType {
    // Section 1 - already speced, original 4
    INACTIVITY,
    STREAK_PRAISE,
    CUSTOM_REMINDER,
    CHAPTER_PROGRESS,

    // Section 2 - new local-only
    STREAK_AT_RISK,
    STREAK_FREEZE_USED,
    QUEST_EXPIRING,
    QUEST_REWARD_UNCLAIMED,
    WEEKLY_XP_GOAL_CLOSE,
    AVATAR_UNLOCK_EXPIRING,

    // Section 4 - server-triggered (Phase 2), simulated locally in this demo
    LEAGUE_ENDING_SOON,
    LEAGUE_PROMOTION_DEMOTION,
    OVERTAKEN_IN_LEAGUE,
    FRIEND_MILESTONE,
    FRIEND_CHEERED_YOU,
    REFERRAL_REWARD_GRANTED,
    NEW_LEAGUE_SEASON,
}

data class NotificationCase(
    val type: NotificationType,
    val category: NotificationCategory,
    val chipLabel: String,
    val title: String,
    val body: String,
    val primaryActionLabel: String,
    val deepLinkRoute: String,
    val deepLinkParams: Map<String, String> = emptyMap(),
    val highPriority: Boolean = false,
    val serverTriggeredInProd: Boolean = false,
)

object NotificationCatalog {

    val cases: List<NotificationCase> = listOf(
        // --- Section 1: already speced ---
        NotificationCase(
            type = NotificationType.INACTIVITY,
            category = NotificationCategory.REMINDERS,
            chipLabel = "REMINDER",
            title = "We miss you!",
            body = "It's been 5 days - come back and continue learning.",
            primaryActionLabel = "Go to App",
            deepLinkRoute = "home",
        ),
        NotificationCase(
            type = NotificationType.STREAK_PRAISE,
            category = NotificationCategory.STREAKS,
            chipLabel = "STREAK",
            title = "12-day streak! You're on fire",
            body = "Amazing! 12 days in a row - don't break the chain now.",
            primaryActionLabel = "View Streak",
            deepLinkRoute = "streak",
        ),
        NotificationCase(
            type = NotificationType.CUSTOM_REMINDER,
            category = NotificationCategory.REMINDERS,
            chipLabel = "REMINDER",
            title = "Time for your daily study session",
            body = "Let's go - pick up where you left off.",
            primaryActionLabel = "Start Now",
            deepLinkRoute = "home",
        ),
        NotificationCase(
            type = NotificationType.CHAPTER_PROGRESS,
            category = NotificationCategory.REMINDERS,
            chipLabel = "REMINDER",
            title = "Continue where you left off",
            body = "You're making good progress on Photosynthesis in Science.",
            primaryActionLabel = "Continue",
            deepLinkRoute = "chapter",
            deepLinkParams = mapOf("chapterId" to "photosynthesis", "subject" to "science"),
        ),

        // --- Section 2: new local-only ---
        NotificationCase(
            type = NotificationType.STREAK_AT_RISK,
            category = NotificationCategory.STREAKS,
            chipLabel = "STREAK",
            title = "Your streak ends in 3 hours",
            body = "Hi Aanya, your 12-day streak is still alive. Finish one activity to keep it going.",
            primaryActionLabel = "Continue Streak",
            deepLinkRoute = "streak",
            highPriority = true,
        ),
        NotificationCase(
            type = NotificationType.STREAK_FREEZE_USED,
            category = NotificationCategory.STREAKS,
            chipLabel = "STREAK",
            title = "A streak freeze saved your chain",
            body = "You missed yesterday, but a freeze token covered it - your streak is safe.",
            primaryActionLabel = "View Streak",
            deepLinkRoute = "streak",
        ),
        NotificationCase(
            type = NotificationType.QUEST_EXPIRING,
            category = NotificationCategory.QUESTS,
            chipLabel = "QUEST",
            title = "Today's quest resets at midnight",
            body = "You haven't started today's quest yet - it's quick, promise.",
            primaryActionLabel = "View Quest",
            deepLinkRoute = "quest",
            deepLinkParams = mapOf("questId" to "daily-1"),
        ),
        NotificationCase(
            type = NotificationType.QUEST_REWARD_UNCLAIMED,
            category = NotificationCategory.QUESTS,
            chipLabel = "QUEST",
            title = "Today's quest reward is waiting",
            body = "You finished today's quest - 20 gems are ready to claim.",
            primaryActionLabel = "Claim",
            deepLinkRoute = "quest",
            deepLinkParams = mapOf("questId" to "daily-1", "claim" to "true"),
        ),
        NotificationCase(
            type = NotificationType.WEEKLY_XP_GOAL_CLOSE,
            category = NotificationCategory.QUESTS,
            chipLabel = "QUEST",
            title = "40 XP from this week's goal",
            body = "One more concept and you'll hit your weekly XP goal.",
            primaryActionLabel = "View Progress",
            deepLinkRoute = "progress",
        ),
        NotificationCase(
            type = NotificationType.AVATAR_UNLOCK_EXPIRING,
            category = NotificationCategory.AVATAR,
            chipLabel = "AVATAR",
            title = "This week's avatar unlock ends tonight",
            body = "The Astronaut avatar rotates out at midnight - use it or share it now.",
            primaryActionLabel = "View Avatar",
            deepLinkRoute = "avatarStudio",
        ),

        // --- Section 4: server-triggered (Phase 2), simulated locally here ---
        NotificationCase(
            type = NotificationType.LEAGUE_ENDING_SOON,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "LEAGUE",
            title = "Your league ends in 2 hours",
            body = "You're in 4th place in the Gold league - finish strong.",
            primaryActionLabel = "View League",
            deepLinkRoute = "league",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.LEAGUE_PROMOTION_DEMOTION,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "LEAGUE",
            title = "You've been promoted to Gold!",
            body = "Great week - you moved up a league. New cohort starts today.",
            primaryActionLabel = "View League",
            deepLinkRoute = "league",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.OVERTAKEN_IN_LEAGUE,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "LEAGUE",
            title = "Someone passed you in your league",
            body = "You dropped to 6th place - a quick session could get you back up.",
            primaryActionLabel = "View League",
            deepLinkRoute = "league",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.FRIEND_MILESTONE,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "FRIENDS",
            title = "Your friend hit a 7-day streak",
            body = "Send a cheer to celebrate their progress.",
            primaryActionLabel = "View Friends",
            deepLinkRoute = "friends",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.FRIEND_CHEERED_YOU,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "FRIENDS",
            title = "You got a cheer!",
            body = "A friend cheered your streak update.",
            primaryActionLabel = "View Friends",
            deepLinkRoute = "friends",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.REFERRAL_REWARD_GRANTED,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "FRIENDS",
            title = "You both earned 50 gems!",
            body = "Your invited friend finished their first concept.",
            primaryActionLabel = "View Friends",
            deepLinkRoute = "friends",
            serverTriggeredInProd = true,
        ),
        NotificationCase(
            type = NotificationType.NEW_LEAGUE_SEASON,
            category = NotificationCategory.LEAGUES_SOCIAL,
            chipLabel = "LEAGUE",
            title = "A new league season just started",
            body = "You've been placed in a fresh cohort - first XP of the week counts double toward rank.",
            primaryActionLabel = "View League",
            deepLinkRoute = "league",
            serverTriggeredInProd = true,
        ),
    )
}
