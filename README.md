# AutoScroller Lite

Мінімальний автоскролер для Android без реклами, аналітики та акаунтів.

## Автооновлення

Застосунок перевіряє останній GitHub Release, завантажує новіший APK через системний DownloadManager і перед встановленням перевіряє package name, versionCode та цифровий підпис. Android все одно показує системне підтвердження встановлення оновлення.

Для автооновлення використовуються лише два дозволи:
- `android.permission.INTERNET` — перевірка GitHub Releases;
- `android.permission.REQUEST_INSTALL_PACKAGES` — передача перевіреного APK системному інсталятору.

## CI

GitHub Actions запускає unit tests, strict Android Lint, debug/release build, перевірку runtime-залежностей, аудит APK, CodeQL та smoke-test на Android 16 emulator. Stable APK підписується постійним ключем, зашифрованим у репозиторії; пароль зберігається тільки в GitHub Actions secret `SIGNING_PASSPHRASE`.
