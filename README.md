# AI Phone Assistant

AI-агент для управления Android-телефоном голосовыми и текстовыми командами.

## Возможности

- 🎤 Голосовые команды на русском языке
- 📱 Управление любыми приложениями
- 🤖 AI-анализ экрана через Groq API
- ⚡ Автоматическое выполнение действий

## Настройка

### 1. Получите API Key

1. Зарегистрируйтесь на [Groq Console](https://console.groq.com)
2. Создайте API Key
3. Вставьте ключ в настройках приложения

### 2. Включите разрешения

1. **Accessibility Service** - для управления UI
2. **Overlay Permission** - для плавающей кнопки
3. **Microphone** - для голосовых команд

## Примеры команд

- "Открой WhatsApp и напиши маме 'Привет'"
- "Позвони Ивану"
- "Открой YouTube и найди музыку для работы"
- "Сделай скриншот"
- "Включи Wi-Fi"

## Сборка

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

## Технологии

- Kotlin + Coroutines
- Jetpack Compose + Material 3
- Hilt для DI
- Room + DataStore
- Retrofit + OkHttp
- Groq API (Llama 3.3)

## Структура проекта

```
app/src/main/java/com/aiassistant/
├── di/                    # Dependency Injection (Hilt)
├── data/                  # Data Layer
│   ├── api/              # API модели и интерфейсы
│   ├── local/            # Room Database
│   └── repository/       # Repository implementations
├── domain/               # Domain Layer
│   ├── model/           # Business models
│   └── repository/      # Repository interfaces
├── presentation/         # UI Layer
│   ├── viewmodel/       # ViewModels
│   └── ui/theme/        # Compose Theme
└── service/             # Services
    ├── accessibility/   # Accessibility Service
    ├── overlay/         # Floating Button
    └── speech/          # Speech Recognition & TTS
```

## Лицензия

MIT License
