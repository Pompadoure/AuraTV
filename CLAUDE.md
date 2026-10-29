# CLAUDE.md: rules for agents working on Aura TV

Aura TV is Johan's personal fork of [OwnTV](https://github.com/ahXN00/OwnTV) (Android TV IPTV player, GPLv3).
Repository: **Pompadoure/AuraTV**. Rules below are carried over from his JoTube project.

## Hard rules
- Push only to Pompadoure/AuraTV, never to the upstream (ahXN00/OwnTV, OwnTV_Core, OwnTV_libmpv).
- **Nothing is installed on any device until Johan says so.** Do not start videos, launch apps or send input
  to his TV/Streamer without his permission. Reading logs/status is fine.
- The signing key and keystore never go into the repo. CI reads GitHub secrets `KEYSTORE_BASE64`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Never print or log them. Without them CI builds a debug APK.
- Never commit an icon or logo showing Johan's face.
- Root causes from evidence (code, CI logs, device logs). No guessing, no "try and see" fixes.
- A change is not done until CI is green; say what was and was not tested on a device.
- Reports to Johan are **short and in Swedish**. Code, commits and docs are in English.
- Keep GPLv3, the LICENSE file and the credit to OwnTV in README.md.

## How the project is put together
- `applicationId` is `tv.aura.auratv`; the Kotlin namespace stays `tv.own.owntv` on purpose (no mass rename,
  easy to merge upstream). The app installs next to the original OwnTV.
- Half of the app lives in OwnTV_Core (database, sync, parsers, EPG, players, **all strings**), consumed as a pinned
  Maven artifact (`gradle/libs.versions.toml`, `owntvCore`). Strings or engine fixes there are not editable from
  this repo; override a resource by name in `app/src/main/res` instead (this is how `app_name` and the icons work).
- The in-app updater and the About page point at `Pompadoure/AuraTV` (`OwnTVApp.kt`, `SettingsScreen.kt`).
- Brand assets are generated: edit `tools/brand/render_aura.py`, run
  `python3 tools/brand/render_aura.py --res app/src/main/res --extras extras/brand/aura`, commit the output.
  Never edit the generated icon, banner or splash files by hand. The launcher icon overrides OwnTV's
  `owntv_icon_eggshell` resources by name, so the manifest is unchanged.
- CI: `.github/workflows/android.yml` runs unit tests and lint, then builds the APK on every push to `main`.
  Tag `v*` publishes a release (`AuraTV.apk`).
