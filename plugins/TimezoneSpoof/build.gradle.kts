version = "0.3.0"
description = "Spoofs your timezone. Only in Discord."

aliucord {
    changelog.set(
        """
        # 0.3.0
        * Improved null safety and optimized string checks to prevent obfuscation crashes
        * Toned down UI toast messages for a cleaner, native-feeling experience

        # 0.2.0
        * Rebranded to TimezoneSpoof
        * Fixed UI text box hint overlapping bug

        # 0.1.0
        * dev release: It spoofs!
        """.trimIndent(),
    )

    deploy.set(true)
}
