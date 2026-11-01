# Upstream sync log

Weekly review of ahXN00/OwnTV changes. Only improvements that do not conflict with Aura TV's own changes are taken.

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
