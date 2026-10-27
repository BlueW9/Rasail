# Rasail

Free and open-source private messenger for Android.

- End-to-end encryption, zero-knowledge relay (in progress)
- Anonymous identity: no phone number, no email
- Screenshots and screen recording blocked
- License: GPL-3.0-or-later

## Build

Add the Gradle wrapper once (`gradle wrapper --gradle-version 8.10.2`), then:

    ./gradlew assembleRelease

To sign with your own key, copy `keystore.properties.example` to `keystore.properties`
and fill it in. That file and any `.jks` are git-ignored and must never be committed.
