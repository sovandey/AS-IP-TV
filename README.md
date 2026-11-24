# AS IP TV

Android IPTV player (Kotlin, Media3/ExoPlayer). Plays HLS (.m3u8) and MPEG-TS streams.

## Features
- 44 built-in Bengali/Bangla channels (`app/src/main/assets/channels.m3u`)
- Category chips (News / Entertainment / Music / Movies), search, favorites (star)
- Add any M3U playlist URL from the menu (⋮ → Add playlist URL); playlists are cached for offline use
- Fullscreen player with Prev/Next channel and Retry

## Build
1. Install Android Studio (Ladybug or newer) and open this folder (`AS-IP-TV`).
2. Wait for Gradle sync. It downloads the Android SDK parts it needs.
3. Plug in a phone (USB debugging on) or start an emulator, then press Run.

To get an APK file instead: **Build → Build APK(s)**. Or from a terminal in this folder:

    ./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
    ./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk

(The release build is signed with the debug key so it installs on your own phone. For Google Play, add your own keystore.)

## Editing channels
Add or edit entries in `app/src/main/assets/channels.m3u`:

    #EXTINF:-1 tvg-logo="https://…/logo.png" group-title="News",Channel Name
    https://stream-url/index.m3u8

## Build the APK without installing anything (GitHub Actions)
1. Create a free GitHub account and a new empty repository.
2. Upload everything in this folder (including the hidden `.github` folder) to it.
3. Open the repository's **Actions** tab → **Build APK** → wait about 5 minutes for the green tick.
4. Open that run and download **AS-IP-TV-apk** under *Artifacts*. Unzip it to get `app-debug.apk`, then install it on your phone.
