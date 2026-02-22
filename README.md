# Aliucord Plugins

A repository of custom plugins for [Aliucord](https://github.com/Aliucord)

## Available Plugins

### TimezoneSpoof

Allows you to override the system timezone exclusively within the Discord app.

**Current Version:** 0.3.0

**Features:**
- Specify any valid IANA Timezone ID (e.g., `America/New_York`, `Asia/Tokyo`, `Etc/UTC`).
- Overrides the JVM timezone at app startup without affecting the rest of your device.
- Reverts cleanly to the system default when cleared.

## Installation

1. **[Download TimezoneSpoof.zip](https://github.com/FurqanHun/aliucord-plugins/raw/builds/TimezoneSpoof.zip)** directly from the builds branch.
2. On your Android device, open your file manager and navigate to your internal storage.
3. Locate the `Aliucord/plugins` directory.
4. Move the downloaded `TimezoneSpoof.zip` file directly into the `plugins` folder. **(Do not extract the .zip file).**
5. Restart the Discord application (if you had it opened in background).
6. Navigate to **Settings** > **Plugins** in Discord to enable and configure TimezoneSpoof.
7. After spoofing the timezone, you would have to restart the Discord application to render the changes.

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
