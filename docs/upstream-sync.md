# Upstream sync log

Weekly review of ahXN00/OwnTV changes. Only improvements that do not conflict with Aura TV's own changes are taken.

## 2026-11-08

Range: bc4cab5d..cce761ff (126 commits, 117 files, v5.2.1-213). Core pin upstream moved 1.0.69 -> 1.0.70.

Taken: nothing.

Skipped (all RISKY, one reason shared by the whole range):
- The new app code is written against OwnTV_Core 1.0.70 (99 commits, 306 files ahead of 38f30e6, the base of
  AuraTV_Core `aura`). Our core fork does not have those APIs, so no app change can be taken without first merging
  upstream core into `aura`, which would be a large core merge on its own and should be a separate, deliberate task.
- It rewrites the places where Aura TV differs: LiveViewModel/LiveScreen/LiveStage (zapping follows the displayed
  category list), StageRail (nav rail labels when focused), HomeScreen/HomeViewModel (Start page with Live now card and
  favourites), EpgScreen/EpgViewModel, plus Live TV category memory and "OK previews first" behaviour changes.
- Features in the range: auto start on boot/wake, Settings lock and protected profiles, profile-based restore and
  Local sync auto-sync, Program details page, two-pane channel browser, Sources button, catch-up guide past days,
  per-profile category memory, rail widening, live edge after TV off. Each is a new feature or behaviour change that
  needs the new core; candidates for later hand-porting once core is merged.

Last upstream commit: cce761ff

## 2026-11-01

Range: 2de7883a..bc4cab5d (1 commit)

Taken: nothing.

Skipped:
- bc4cab5d "Guide Back focuses the playing channel, moved items keep their place, delete asks first" (RISKY).
  It was built against OwnTV_Core 236cecaa ("core's new rule" for Moves), which AuraTV_Core (based on 38f30e6)
  does not have, so the app code would not match our core. It also rewrites the zap lists and paging in
  Live/Movies/Series view models and EpgViewModel/EpgScreen, which is exactly where Aura TV makes zapping follow
  the displayed category list, and it changes Move/sort behaviour. Worth porting by hand later, piece by piece
  (small independent parts: Downloads delete confirmation, Series "0 min" fix) once core is merged.

Last upstream commit: bc4cab5d8adb70fd4a476b8d8e007050abb295e5
