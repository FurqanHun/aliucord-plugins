# Aliucord Plugins

A repository of custom plugins for [Aliucord](https://github.com/Aliucord).

> [!TIP]
> Looking for AMOLED themes? Check out my [Aliucord Themes](https://github.com/FurqanHun/aliucord-themes) repository (featuring **AdoMoled** - A handcrafted pure AMOLED theme inspired by adomination, and **AmAmAmEx** - Pure AMOLED theme featuring **Material You (Monet)** dynamic wallpaper accents.)!

## Available Plugins

### LetMeRead

Dynamically adjusts contrast and readability for unreadable usernames, roles, and chat across any theme, wallpaper, or background.

**Current Version:** 0.2.0

**Features:**
- Automatically adjusts low-contrast role and username colors to ensure readability on any background while preserving their original hue.
- Seamless coverage across chat message headers, reply previews, and the member list drawer.
- Built-in 2D spatial sampling across custom theme wallpapers to prevent text from blending into background artwork.
- Opt-in contrast adjustment for message body text on poorly contrasted custom themes.
- Configurable contrast threshold slider and adjustment modes in plugin settings.

### TimezoneSpoof

Allows you to override the system timezone exclusively within the Discord app.

**Current Version:** 0.4.0

**Features:**
- Native in-app search dropdown to easily find valid IANA Timezone IDs (e.g., `America/New_York`, `Asia/Tokyo`, `Etc/UTC`).
- Built-in validation to prevent typos and accidental JVM fallback.
- Overrides the JVM timezone at app startup without affecting the rest of your Android device.
- Reverts cleanly to the system default when cleared.

## Installation

### Recommended Method (Official)
Plugins are hosted in the Aliucord repository! You can install them natively via the Plugin Web or:
1. Go to the **Aliucord Discord Server**.
2. Navigate to the `#new-plugins` or `#plugin-list` channel.
3. Search for **LetMeRead** or **TimezoneSpoof** and install directly into your app.
4. Restart Discord, then head to **Settings > Plugins** to configure.

### Manual Installation (For Sideloading)
1. Download the desired plugin zip directly from the builds branch:
   - **[Download LetMeRead.zip](https://github.com/FurqanHun/aliucord-plugins/raw/builds/LetMeRead.zip)**
   - **[Download TimezoneSpoof.zip](https://github.com/FurqanHun/aliucord-plugins/raw/builds/TimezoneSpoof.zip)**
2. On your Android device, open your file manager and navigate to your internal storage.
3. Locate the `Aliucord/plugins` directory.
4. Move the downloaded `.zip` file directly into the `plugins` folder. **(Do not extract the .zip file).**
5. Restart the Discord application.
6. Navigate to **Settings > Plugins** in Discord to enable and configure the plugin.

*Note: For TimezoneSpoof, after applying or clearing a spoofed timezone, you must restart the Discord application for changes to take effect in the JVM.*

## Building from Source
To compile these plugins locally, ensure you have JDK 21 and a standard Gradle environment configured.
```bash
git clone https://github.com/FurqanHun/aliucord-plugins.git
cd aliucord-plugins
./gradlew :LetMeRead:make
./gradlew :TimezoneSpoof:make
```
The compiled plugin will be generated as a `.zip` file located in the `plugins/<PluginName>/build/outputs/` directory.

## License

All the plugins are licensed under the [GPL v3 License](LICENSE)
