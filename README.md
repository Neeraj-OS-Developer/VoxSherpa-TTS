<div align="center">

<img src="https://raw.githubusercontent.com/CodeBySonu95/VoxSherpa-TTS/main/fastlane/metadata/android/en-US/images/featureGraphic.png" width="100%" alt="VoxSherpa TTS Banner"/>

<br/>
<br/>

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0--beta-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://huggingface.co/CodeBySonu95/Sherpa-onnx-models/resolve/main/VoxSherpa-TTS_test.apk)
[![F-Droid](https://img.shields.io/badge/F--Droid-Coming%20Soon-orange?style=for-the-badge&logo=fdroid&logoColor=white)](https://f-droid.org)
[![Android](https://img.shields.io/badge/Android-11%2B-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![License](https://img.shields.io/badge/License-GPL%20v3.0-blue?style=for-the-badge)](LICENSE)
[![Sherpa-ONNX](https://img.shields.io/badge/Powered%20by-Sherpa--ONNX-orange?style=for-the-badge)](https://github.com/k2-fsa/sherpa-onnx)

<h1>VoxSherpa TTS</h1>
<h3>Studio-quality offline neural text-to-speech for Android.<br/>Hindi · English · Japanese · Chinese · and more — No cloud. No limits. No compromise.</h3>

</div>

---

## 📥 Download

**v1.0-beta is now available!**

> First public beta. Core TTS engines (Kokoro, Piper, VITS) are fully functional. Model download and import supported.

| Variant | Size | Link |
|---------|------|------|
| **Universal** | ~80 MB | [⬇️ Download APK v1.0-beta](https://huggingface.co/CodeBySonu95/Sherpa-onnx-models/resolve/main/VoxSherpa-TTS_test.apk) |

> **Why this size?** The APK includes the Sherpa-ONNX inference engine. TTS models (Kokoro, Piper, VITS) are downloaded separately inside the app — so you only download what you need.

---

## 🎯 Why VoxSherpa?

Most TTS apps make you choose between **quality** and **privacy**. Cloud-based tools like ElevenLabs sound incredible — but they require internet, send your text to remote servers, and charge per character.

**VoxSherpa breaks that tradeoff.**

It runs professional-grade neural engines entirely on your device — supporting **Hindi, English, British English, Japanese, Chinese, and 50+ more languages** — all without any internet connection.

| Engine | Quality | Speed | Best For |
|--------|---------|-------|----------|
| 🧠 **Kokoro-82M** | Studio-grade · rivals ElevenLabs | Slower on budget hardware | Audiobooks, voiceovers, professional content |
| ⚡ **Piper / VITS** | Natural · clear | Fast on any device | Daily use, quick synthesis |

---

## 📸 Screenshots

<div align="center">

| Generate | Models | Library | Settings |
|:---:|:---:|:---:|:---:|
| <img src="https://raw.githubusercontent.com/CodeBySonu95/VoxSherpa-TTS/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="180"/> | <img src="https://raw.githubusercontent.com/CodeBySonu95/VoxSherpa-TTS/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="180"/> | <img src="https://raw.githubusercontent.com/CodeBySonu95/VoxSherpa-TTS/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="180"/> | <img src="https://raw.githubusercontent.com/CodeBySonu95/VoxSherpa-TTS/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="180"/> |

</div>

---

## ✨ Features

### 🌐 Multilingual Neural TTS
- **Hindi** · **English** · **British English** · **Japanese** · **Chinese** · Spanish · French · and 50+ more
- Kokoro-82M supports 100+ speaker voices across languages
- First offline Android TTS with serious **Hindi language support**

### 🎙️ Dual Neural Engine
- **Kokoro-82M** — 82 million parameter neural model. Same architecture used by top-tier commercial TTS services.
- **Piper / VITS** — Fast, lightweight, natural. Generates speech in seconds on any Android device.

### 🔒 100% Offline & Private
- All processing happens on your device
- No internet required after model download
- No account, no telemetry, no data collection
- Your text never leaves your phone

### 📦 Flexible Model Management
- Download models directly from inside the app
- Import your own `.onnx` models from local storage
- Multiple models installed simultaneously
- Smart storage tracking

### 🎧 Audio Controls
- Real-time waveform visualization
- Adjustable speed and pitch
- Play, pause, and replay generated audio
- Export as WAV with correct sample rate per model

### 📚 Speech Library
- Save all generated audio locally
- Favorites system for quick access
- View generation history with timestamps
- Voice model attribution per recording

### ⚙️ Smart Settings
- **Smart Punctuation** — natural pauses after sentence breaks
- **Emotion Tags** — `[whisper]`, `[angry]`, `[happy]` support
- Per-model voice selection
- Theme-aware UI

---

## ⚡ Performance

Generation speed depends on your device's processor. Kokoro prioritizes **quality over speed** by design — running an 82M parameter model fully offline on mobile CPU is genuinely pushing hardware limits.

| Device Tier | Kokoro | Piper / VITS |
|-------------|--------|--------------|
| 🟢 Flagship (Snapdragon 8 Gen 3) | ~20–40 sec/min audio | ~5 sec/min audio |
| 🟡 Mid-range (8-core) | ~60–90 sec/min audio | ~10 sec/min audio |
| 🔴 Budget (6-core) | ~2–3 min/min audio | ~20 sec/min audio |

> **Tip:** For quick synthesis on any device, use **Piper or VITS**. Switch to **Kokoro** when you need studio-quality output and can wait.

---

## 🛠️ Technical Architecture

```
User Text
    │
    ├─── Kokoro Engine (KokoroEngine.java)
    │         └── Sherpa-ONNX JNI → ONNX Runtime → CPU
    │                   └── kokoro-multi-lang-v1_0 (82M params, FP32)
    │
    └─── Piper / VITS Engine (VoiceEngine.java)
              └── Sherpa-ONNX JNI → ONNX Runtime → CPU
                        └── VITS model (language-specific)
```

**Built with:**
- [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) — on-device neural inference
- [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) — multilingual neural TTS model
- [Piper](https://github.com/rhasspy/piper) — fast local TTS
- Android AudioTrack API — low-latency PCM playback

> **On Hardware Acceleration**: NNAPI was investigated but is blocked at the compiled `.so` level in the current Sherpa-ONNX build. CPU inference with optimal thread count is used instead and performs reliably across devices.

---

## 📲 Model Import (Advanced Users)

VoxSherpa supports importing any Sherpa-ONNX compatible TTS model:

1. Place your `.onnx` model + `tokens.txt` on device storage
2. Open **Models tab** → tap **+** → **Import Local Model**
3. Select your files and start generating

---

## 🤝 Contributing

VoxSherpa is open source. Contributions welcome:

- 🐛 Bug reports via [Issues](../../issues)
- 💡 Feature requests via [Discussions](../../discussions)
- 🔧 Pull requests for fixes and improvements

---

## 📜 License

```
Copyright (C) 2025 CodeBySonu95

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

https://www.gnu.org/licenses/gpl-3.0.html
```

---

## 🙏 Acknowledgements

- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) — the inference engine that makes this possible
- [hexgrad/Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) — the neural model behind studio-quality synthesis
- [rhasspy/piper](https://github.com/rhasspy/piper) — fast local TTS engine

---

<div align="center">

**Built with obsession. Runs without internet.**

*VoxSherpa — Because your voice deserves to stay yours.*

</div>
