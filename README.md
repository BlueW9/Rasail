# Rasail

Free and open-source private messenger for Android.

- End-to-end encryption (Signal protocol via libsignal), zero-knowledge relay (in progress)
- Anonymous identity: no phone number, no email
- Screenshots and screen recording blocked
- License: GPL-3.0-or-later

## Third-party

- [libsignal](https://github.com/signalapp/libsignal) (AGPL-3.0), Signal protocol implementation
- [ZXing core](https://github.com/zxing/zxing) (Apache-2.0), QR code encoding

## Build

Built by GitHub Actions (`.github/workflows/build.yml`), which produces an unsigned APK.
Sign it yourself with your own key:

    apksigner sign --ks <your.jks> --out Rasail.apk app-release-unsigned.apk

`keystore.properties` and any `.jks` are git-ignored and must never be committed.
