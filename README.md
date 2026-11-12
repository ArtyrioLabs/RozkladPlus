<p align="center">
  <img src="art/preview.png" alt="Rozklad+ Preview" width="100%" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Design-Material_3_Expressive-8AB4F8?style=flat-square&logo=materialdesign&logoColor=white" alt="Material 3 Expressive" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Version-v1.8.7-blue?style=flat-square" alt="Version 1.8.7" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-lightgrey?style=flat-square" alt="License" />
</p>

---

# Rozklad+ (Розклад+)

**Розклад+** — швидкий, надійний та автономний розклад занять для Android, спроєктований за гайдлайнами **Material 3 Expressive** з підтримкою динамічної палітри кольорів **Material You (Monet)**.

Застосунок розроблено з фокусом на практичну користь у реальному навчальному процесі: миттєвий запуск без мережі, точний розрахунок часу, відсутність візуального шуму та глибока енергоефективність.

---

## Основні можливості

### Дизайн та інтерфейс
- **Ієрархія розкладу**: великий контрастний час початку пари, вторинний час завершення, компактні бейджі типу занять (`Лекція`, `Практика`, `Лабораторна`), аудиторія та статус `Онлайн`.
- **Динамічний Monet**: безшовна адаптація палітри інтерфейсу та адаптивної іконки на робочому столі під шпалери системи (Android 12+).
- **Сфокусована темна тема**: вивірений контраст контейнерів без пересвіту для комфортної роботи ввечері та в аудиторіях з низьким освітленням.

### Інтелектуальний розклад та перерви
- **Аналіз вікон**: автоматичний підрахунок тривалості великих перерв і вільних вікон між парами із зазначенням часу до початку наступного заняття.
- **Прямі посилання на онлайн-пари**: автоматичне отримання посилань на відеоконференції (Zoom, Google Meet, MS Teams, Moodle) напряму з оголошень розкладу університету з миттєвим переходом за натисканням на картку заняття.

### Віджет робочого столу (Jetpack Glance)
- Сучасний віджет у стилі системних інформерів Google Pixel.
- Підтримка світлої й темної тем, динамічних кольорів Monet та автоперемикання на наступний навчальний день після завершення пар.

### Сповіщення та живий таймер
- Попередження за 15 та 5 хвилин до пари через енергоефективний `ExactAlarmScheduler`.
- Живий статус у шторці з таймером зворотного відліку до кінця поточної пари без зайвого споживання батареї.

### Надійність та приватність
- **Повна автономність (Offline-first)**: збереження локального кэшу розкладу — додаток функціонує навіть за повної відсутності зв'язку.
- **Безпечне зберігання**: конфіденційні параметри захищені через `EncryptedSharedPreferences` та апаратний Android Keystore.
- **Вбудоване OTA-оновлення**: безпечна доставка релізних оновлень безпосередньо в додатку.

---

## Архітектура та стек технологій

- **Мова**: Kotlin 2.0 (Coroutines, Flow, StateFlow)
- **UI Framework**: Jetpack Compose (BOM 2025.09), Material 3 (`1.5.0-alpha01`)
- **Віджети**: AndroidX Glance (`1.1.1`)
- **Архітектурний патерн**: Clean Architecture + MVVM + UDF (Unidirectional Data Flow)
- **DI**: Dagger Hilt
- **Мережа**: Retrofit 2, OkHttp 4, Gson
- **Фонові процеси**: WorkManager, Foreground Services, Exact Alarm Manager

---

## Збірка з вихідного коду

### Вимоги
- Android Studio Ladybug (2024.2.1) або новіша версія
- JDK 17
- Android SDK 35 (Android 15)

### Інструкція
1. Клонуйте репозиторій:
   ```bash
   git clone https://github.com/ArtyrioLabs/RozkladPlus.git
   cd RozkladPlus
   ```

2. Створіть файл підпису (опціонально, для Release):
   Скопіюйте `keystore.properties.example` у `keystore.properties` та вкажіть параметри вашого ключа:
   ```properties
   storeFile=your-keystore.jks
   storePassword=your-store-password
   keyAlias=your-key-alias
   keyPassword=your-key-password
   ```

3. Зберіть проєкт через Gradle:
   ```bash
   # Debug APK
   ./gradlew :app:assembleDebug

   # Release APK (підписаний)
   ./gradlew :app:assembleRelease
   ```

---

## Встановлення

Готові підписані APK-файли публікуються у розділі [Releases](https://github.com/ArtyrioLabs/RozkladPlus/releases).

---

## Зворотний зв'язок

Якщо ви помітили помилку або маєте ідею щодо покращення функціоналу, створіть новий [Issue](https://github.com/ArtyrioLabs/RozkladPlus/issues) із детальним описом.

---

## Ліцензія

Проєкт розповсюджується за ліцензією [Apache License 2.0](LICENSE).
