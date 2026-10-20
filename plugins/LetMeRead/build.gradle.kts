version = "0.2.0"
description = "Dynamically adjusts contrast and readability for unreadable usernames, roles, and chat across any theme, wallpaper, or background."

aliucord {
    changelog.set(
        """
        # 0.2.0
        * Added opt-in contrast adjustment for chat message content and reply previews
        * Added 2D spatial wallpaper sampling across vertical and horizontal gradients
        * Improved member list sidebar background resolution for custom themes
        * Added perceptual human-eye contrast boost for blue and purple roles
        * Added dynamic saturation balancing to prevent washed-out or neon colors
        * Added real-time readability tier indicators to the settings contrast slider
        
        # 0.1.0-dev
        * Added dynamic contrast adjustment for role and author colors
        * Added smart HSL lightness boost to preserve original role hue
        * Added slider to configure contrast threshold in settings
        * Added support for custom wallpapers and themes
        """.trimIndent(),
    )
}
