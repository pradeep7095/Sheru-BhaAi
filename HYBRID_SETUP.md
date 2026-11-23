# Sheru Bhai — hybrid AI integration (v0.6 foundation)

## Implemented in the Android app
- Text and speech input (Android recognizer); TextToSpeech output
- Local commands: flashlight, app launching, volume, Settings
- User-initiated Android share / process-text intake
- Editable reply drafts with explicit review and Android share chooser
- HTTPS AI chat and AI-assisted reply drafts through a separately deployed server
- No provider API key in the APK

## AI server deployment
Deploy `backend/` to a Python HTTPS host with start command:
`uvicorn main:app --host 0.0.0.0 --port $PORT`
Set environment variables **on the server only**:
- `GEMINI_API_KEY` — provider key
- `SHERU_ACCESS_TOKEN` — long random token for app authentication
- `GEMINI_MODEL` — optional, defaults to `gemini-2.5-flash`

Configure the Android app's AI server URL as `https://your-host/chat` and enter the matching access token. Do not put the Gemini key into the app. The app sends the user's question and currently shared text only when **Ask AI** is tapped. Protect the endpoint with per-user authentication and rate limiting before production. The prototype stores the access token in app-private preferences, not Android Keystore; do not treat this as production credential storage.

## Security and limitations
This release intentionally does not register an Accessibility Service or Notification Listener, which previously triggered Play Protect blocks on sideloaded APKs. It cannot autonomously read all chats, scrape other apps, or send WhatsApp messages. Users must explicitly share text and confirm outgoing content. No SMS/contacts access is requested.

Cactus Needle 3 native Android inference is **not yet integrated**. Offline behavior consists of local command routing, not offline language-model reasoning. Verify ABI, native bindings, model licensing and runtime performance before claiming offline AI support.

The cloud relay requires deployment and secret configuration before AI chat works. A successful GitHub Actions APK build does not verify installation, Play Protect behavior, or AI service operation.
