# AnimeVost Mobile

<div align="center">

[![Release](https://img.shields.io/github/v/release/Mirsmog/animevost-mobile?style=flat-square&color=6c5ce7)](https://github.com/Mirsmog/animevost-mobile/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?style=flat-square&logo=android)](https://github.com/Mirsmog/animevost-mobile/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/github/license/Mirsmog/animevost-mobile?style=flat-square)](LICENSE)

</div>

<div align="center">
  <img src="docs/screenshots/screen_4.jpg" width="22%">
  <img src="docs/screenshots/screen_2.jpg" width="22%">
  <img src="docs/screenshots/screen_1.jpg" width="22%">
  <img src="docs/screenshots/screen_3.jpg" width="22%">
</div>

<br>

**Неофициальный** нативный клиент [animevost.org](https://animevost.org) для Android. Написан на Kotlin + Jetpack Compose. Никакой связи с командой сайта не имеет.

## Возможности

- 🎬 Плеер с жестовым управлением, регулировкой скорости и автопереходом к следующей серии
- ⏭️ Автопропуск интро и концовки через AniSkip
- 📋 Списки просмотра — «Смотрю», «Просмотрено», «Запланировано», «Брошено»
- 🗂️ Каталог с фильтрацией по жанру, типу и году
- 🔍 Поиск по названию
- 📅 Расписание выхода новых серий
- 💬 Комментарии
- ⭐ Избранное

## Скачать

Готовые APK → **[Releases](https://github.com/Mirsmog/animevost-mobile/releases/latest)**

| APK | Для кого |
|---|---|
| `arm64-v8a` | Большинство современных устройств |
| `armeabi-v7a` | Старые 32-битные устройства |
| `universal` | Если не уверены — берите этот |

> Перед установкой разрешите установку из неизвестных источников: **Настройки → Приложения → Особые права → Установка неизвестных приложений**.

## Сборка из исходников

```bash
git clone https://github.com/Mirsmog/animevost-mobile.git
cd animevost-mobile
./gradlew assembleDebug
```

Требования: Android 8.0+ (API 26), JDK 17, Android Studio Hedgehog+.

> Для входа нужен существующий аккаунт на [animevost.org](https://animevost.org).

## Участие в разработке

Пул-реквесты приветствуются. Перед крупными изменениями лучше открыть issue.

## Лицензия

[MIT](LICENSE)
