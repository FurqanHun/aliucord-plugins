# Aliucord Plugins

A repository of custom plugins for [Aliucord](https://github.com/Aliucord)

## Available Plugins

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
TimezoneSpoof is officially accepted in the Aliucord repository! You can install it natively:
1. Go to the **Aliucord Discord Server**.
2. Navigate to the `#plugins` channel or use the Plugin Web.
3. Search for **TimezoneSpoof** and install it directly into your app.
4. Restart Discord, then head to **Settings > Plugins** to configure your spoofed timezone.

### Manual Installation (For Sideloading)
1. **[Download TimezoneSpoof.zip](https://github.com/FurqanHun/aliucord-plugins/raw/builds/TimezoneSpoof.zip)** directly from the builds branch.
2. On your Android device, open your file manager and navigate to your internal storage.
3. Locate the `Aliucord/plugins` directory.
4. Move the downloaded `TimezoneSpoof.zip` file directly into the `plugins` folder. **(Do not extract the .zip file).**
5. Restart the Discord application.
6. Navigate to **Settings > Plugins** in Discord to enable and configure TimezoneSpoof.

*Note: After applying or clearing a spoofed timezone, you must restart the Discord application for the changes to take effect in the JVM.*

## Building from Source
To compile these plugins locally, ensure you have JDK 21 and a standard Gradle environment configured.
```bash
git clone https://github.com/FurqanHun/aliucord-plugins.git
cd aliucord-plugins
./gradlew :TimezoneSpoof:make
```
The compiled plugin will be generated as a `.zip` file located in the `plugins/TimezoneSpoof/build/outputs/` directory.

## License

All the plugins are licensed under the [GPL v3 License](LICENSE)
