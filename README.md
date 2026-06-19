<div align="center">

# 🛒 ShopIQ Pro

**AI-powered billing app for Indian kirana stores**

Built with Kotlin · Jetpack Compose · Gemini AI

</div>

---

## What it does

ShopIQ Pro is a native Android billing app for kirana (neighborhood grocery) store owners. It replaces the notebook-and-calculator workflow with a fast, mobile-first POS that runs entirely on-device, plus AI features powered by the Gemini API.

- **Billing** — create bills, track payment mode (Cash / UPI / Card), itemized totals
- **Inventory** — manage items with barcode, price, cost price, stock, and category
- **Barcode scanning** — scan products straight into a bill using the device camera (ML Kit)
- **Udhar / credit ledger** — track customer outstanding balances, purchases, and repayments
- **Gemini AI integration** — AI-assisted features powered by Google's Gemini API
- **Local-first storage** — all data persists locally via Room, no server required

## Tech stack

| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Single-activity, ViewModel + StateFlow |
| Local DB | Room |
| Networking | Retrofit + OkHttp + Moshi |
| AI | Gemini API |
| Barcode scanning | ML Kit (Barcode Scanning) + CameraX |
| Build | Gradle (Kotlin DSL), AGP, KSP |

## Project structure

```
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── api/          # GeminiApiClient, ClerkApiClient, API models
│   ├── db/            # Room entities (Store, Item, Bill, Customer, CustomerLog)
│   └── repository/     # KiranaRepository
└── ui/
    ├── KiranaScreens.kt
    ├── KiranaViewModel.kt
    └── theme/          # Color, Theme, Type
```

## Getting started

### Prerequisites
- [Android Studio](https://developer.android.com/studio)
- A [Gemini API key](https://aistudio.google.com/apikey)

### Run locally

1. Clone the repo and open it in Android Studio.
2. Let Android Studio sync Gradle and resolve dependencies.
3. Create a `.env` file in the project root and add your key:
   ```
   GEMINI_API_KEY=your_key_here
   ```
   (see `.env.example` for the format)
4. In `app/build.gradle.kts`, remove the debug signing line if it's still pointing at a `debug.keystore` that doesn't exist:
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```
5. Run the app on an emulator or physical device (**Run ▶** or **Build → Build APK(s)**).

### Build a release APK

The release build expects a signing keystore, supplied via environment variables:

```
KEYSTORE_PATH=/path/to/your.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

### CI build (GitHub Actions)

This repo includes `.github/workflows/build-apk.yml`, which builds a debug APK on every push and uploads it as a workflow artifact. To use it, add your key as a repository secret named `GEMINI_API_KEY` (**Settings → Secrets and variables → Actions**).

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Gemini API calls |
| `CAMERA` | Barcode scanning |
| `VIBRATE` | Scan/action feedback |

## Roadmap / ideas

- [ ] WhatsApp bill sharing
- [ ] Voice-based billing
- [ ] Multi-store support
- [ ] Cloud sync / backup

## License

An Internship Project

---

<div align="center">
Built by Prajwal
</div>
