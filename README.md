# 🛡️ PrivacyBrowser - Multi-Identity Anti-Tracking Browser

## Kya hai yeh app?
Ek Android browser jisme aap multiple **fully isolated profiles** bana sakte hain.
Har profile ek alag identity hai - alag cookies, alag fingerprint, alag storage.

---

## Features

### ✅ Profile Isolation (Main Feature)
- Har profile ka **alag cookie store**
- Alag **LocalStorage / SessionStorage**
- Websites ek profile se doosre profile ko **detect nahi kar sakti**
- Android ka `WebView.setDataDirectorySuffix()` use kiya gaya hai

### ✅ Anti-Tracking (50+ tracker block)
- Google Analytics block
- Facebook Pixel block
- DoubleClick/Ads block
- Hotjar, Mixpanel, Amplitude block
- Aur bhi bahut saare...

### ✅ Anti-Fingerprinting
- Har profile ka **alag User-Agent** (alag phone jaisa dikhta hai)
- **Canvas fingerprint** protection
- **WebRTC IP leak** blocked
- Battery API blocked
- Hardware info randomized (RAM, CPU cores)

### ✅ Privacy Settings
- Location access: OFF
- Form auto-save: OFF
- Password save: OFF
- HTTPS only (mixed content blocked)

---

## Android Studio Mein Kaise Import Karein

1. **Android Studio** open karein
2. `File → Open` karein
3. Is folder ko select karein: `PrivacyBrowser/`
4. Gradle sync hone do
5. `Build → Make Project` karein
6. Apne phone pe install karein ya APK banayein:
   `Build → Build Bundle(s)/APK(s) → Build APK(s)`

---

## Requirements
- Android Studio Hedgehog (2023.1.1) ya newer
- Android SDK 34
- Kotlin 1.9+
- Minimum Android 7.0 (API 24)

---

## App Structure

```
PrivacyBrowser/
├── app/src/main/
│   ├── java/com/privacybrowser/
│   │   ├── MainActivity.kt      ← Profile list screen
│   │   └── BrowserActivity.kt   ← Browser with privacy features
│   ├── res/layout/
│   │   ├── activity_main.xml    ← Main screen UI
│   │   ├── activity_browser.xml ← Browser UI
│   │   └── item_profile.xml     ← Profile card UI
│   ├── res/values/
│   │   └── themes.xml           ← Dark theme
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

---

## Aage Kya Add Ho Sakta Hai
- [ ] VPN integration per profile
- [ ] Custom DNS per profile (like 1.1.1.1)
- [ ] Bookmarks per profile
- [ ] Incognito mode within a profile
- [ ] Profile import/export
- [ ] Password manager per profile

---

## Kaise Kaam Karta Hai (Technical)

Android ka `WebView.setDataDirectorySuffix("profile_id")` call karke
system ko batate hain ki is WebView ka data `/data/app/com.privacybrowser/profile_id/`
mein store ho. Is tarah Profile A ka koi bhi data Profile B ko nahi milta.

JavaScript injection se fingerprinting APIs ko override karte hain
taaki websites browser ki real identity track na kar sakein.
