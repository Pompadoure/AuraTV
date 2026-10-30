# Aura TV overnight runbook

This file is the whole briefing for an unattended run. A run starts in a fresh session with no memory of
earlier ones, so everything it needs is here, in `CLAUDE.md`, and in the state file on branch
`overnight-state`. Code, commits and docs are English. The final report to Johan is short and in Swedish.

## Ground rules (from CLAUDE.md, repeated because they matter at night)

- Push code only to `Pompadoure/AuraTV` `main`. Never touch upstream (`ahXN00/*`). Never force-push. No tags
  (the proxy refuses them with 403; CI publishes the rolling `dev` pre-release by itself).
- **Do not install anything on any device, do not start apps or send input to the TV/Streamer.** Johan
  installs in the morning. Reading is fine, but nothing here needs the device at all.
- Root causes from evidence (code, CI annotations). No guess-and-retry fixes.
- A step is done only when CI is green for its commit (tests, lint, `verifyI18nLiterals`, build).
- `main` must be green when the night ends. If a step cannot be made green in 3 attempts, `git revert` its
  commits, push, confirm green, mark the step `BLOCKED` with the evidence, and go on to the next one.
- Keep GPLv3, `LICENSE`, and the credit to OwnTV in README.md and the About page (attribution stays; the
  product name changes).
- Never commit secrets or an icon showing Johan's face. Never print keystore values.
- Use the core pin the way CLAUDE.md describes (CI builds against OwnTV_Core source at `CORE_SHA`). Core
  itself cannot be edited; override resources by name in `app/src/main/res` instead.
- The Kotlin namespace stays `tv.own.owntv`. Do not mass-rename packages.
- Before pushing a task's final change, have a separate reviewer subagent read the diff (correctness,
  focus handling, regressions in other screens) and fix what it finds. No finding is ignored silently.

## How a run works (resume protocol)

State lives in `docs/overnight/PROGRESS.md` on branch **`overnight-state`** (CI does not run on that branch,
so heartbeats cost nothing). Read it with `git fetch origin overnight-state` and
`git show origin/overnight-state:docs/overnight/PROGRESS.md`. Update it by committing to a local checkout of
that branch (`git worktree add ../state overnight-state`) and `git push origin overnight-state`.

1. Attach the repo: `add_repo` Pompadoure/AuraTV with `access: push`, then clone/fetch it. Also
   `add_repo` ahXN00/OwnTV_Core with read access if you need core source (resource names, strings).
   If the push attach is refused, stop and let the run end; the next hourly run tries again.
2. Read `CLAUDE.md`, this file and `PROGRESS.md`.
3. **Lock.** If `status: DONE`, delete the scheduled task named "Aura TV overnight" (list_triggers /
   delete_trigger) and stop. If `heartbeat_utc` is less than 50 minutes old and `lock_owner` is not you,
   another run is alive: stop at once. Otherwise take the lock (`lock_owner`, `heartbeat_utc` now,
   `status: RUNNING`), push.
4. **Resume.** Find the first step that is not `DONE`/`BLOCKED`. If it says `IN_PROGRESS`, a run died in the
   middle of it: check `git log origin/main`, uncommitted leftovers do not exist (fresh session), and CI for
   the latest `main` SHA (`gh run list -R Pompadoure/AuraTV --workflow "Android CI" --commit <sha>`,
   `gh api repos/Pompadoure/AuraTV/check-runs/<job>/annotations`). Continue from what is actually on `main`,
   never from what the notes hope is there.
5. Work one step at a time. Before starting a step, set it `IN_PROGRESS` with the intended SHA/branch and
   push the state. After each push to `main`, record the SHA, then wait for CI. While waiting, refresh the
   heartbeat at least every 10 minutes (an until-loop with `sleep`, or Monitor), so a live run is never
   mistaken for a dead one.
6. Failures: read the CI annotation first (the workflow posts the Kotlin `e:` lines and the Gradle reason).
   Three failed attempts on one step means revert and `BLOCKED` as above.
7. Stop starting new steps after **06:30 Europe/Stockholm**. The run that finishes the last step, or the one
   at the end of the window, writes the `## Morning report` section in `PROGRESS.md` (Swedish, short: what
   is done, what is blocked and why, what is not verified on a device, the APK location) and sets
   `status: DONE` when nothing is left. It must also say which steps need a look on the real TV.
8. The state file is the only memory. Update it after every meaningful event, in English, tersely.

## Build outputs

Every green push to `main` replaces the rolling pre-release
`https://github.com/Pompadoure/AuraTV/releases/tag/dev` (asset `AuraTV-dev.apk`). Johan installs it himself
in the morning, the same way as the first build (HA downloader into `/config/jotube/`, `androidtv.upload`,
`pm install -r`), but only when he says so.

## Tasks (Johan's order)

Nothing below can be checked visually without the TV, so keep UI changes conservative: constants in one
place, no new screens, no behaviour change outside the named area. State exact before/after values in the
report.

### T1 – Channel zapping follows the favourites list

**Wanted:** with the TV guide (Live TV guide view) filtered to **Favorites**, changing channel with
next/previous (CH+/CH−, up/down while a channel plays full screen or in the preview) must go to the next or
previous channel *in the favourites list, in its order*. Today it goes by the provider's order: from
"SVT1 HD SE" it lands on "SVT1 FHD SE" instead of "SVT2 HD SE".

**Find first, with evidence:** where next/previous channel is computed (grep `zap`, `nextChannel`,
`prevChannel`, `channelUp`, `channelDown`, `onZap`, in `features/live`, `features/epg`, `features/shell`,
`player`, and core's `live` package at `CORE_SHA`), and which list that code walks. Confirm that it ignores
the active category/favourites filter, and in which views (list view, guide view, full-screen player,
Multiview). Fix at the root: the zap order must be the list the user is currently browsing (selected
category, including Favorites, custom categories, History) in its displayed order, wrapping at both ends.
Do not change zapping when the category is "All channels".

**Done when:** a unit test (JVM, no device) proves next/previous over a favourites list with duplicates of
the same station in other positions, plus wrap-around; the existing tests still pass; CI green.

### T2 – Channel list is easier to read

**Wanted:** the channel list (and the guide rows if they share the same row component) is hard to read. Give
it air so Johan can navigate and recognise programmes more easily.

**Approach:** read `USER_GUIDE.md` "What the row tells you" and the row composables. Then, conservatively:
larger row height and vertical padding, clearer separation between rows (spacing or a subtle divider), a
clearer title/subtitle hierarchy (channel name stronger, programme line weaker), consistent column
alignment (number, logo, name, programme, time left), and a clearly visible focus state. Do not remove
information. Respect the existing density/size settings if there are any (look in Settings → Layout) and
scale from them rather than hard-coding. Make sure TV safe-area and the 8-ish rows visible at 1080p still
make sense; fewer rows per screen is the accepted trade-off, but not fewer than about 7.

**Done when:** CI green; the change is small enough to review as one commit; the report lists the changed
constants.

### T3 – Side menu expands with labels while focused

**Wanted:** when focus enters the left rail, it expands and shows a text label for every section; it opens
**on top of** whatever is next to it (channel list, settings, anything), without pushing or resizing it.
When focus leaves the rail it collapses back to icons only, as today.

**Approach:** `features/shell/components/StageRail.kt` already has `RailState` (HIDDEN/OPEN/PINNED/…) and
`NavSize`. First find out what already exists and what Settings → Layout offers, then make the *default*
behave as wanted: collapsed icons-only at rest, expanded overlay with labels while the rail has focus, back
to icons when focus moves into the content. Overlay means no change to the content's layout width. Keep
D-pad behaviour: Left from content opens it, Right or Back closes it. Do not break the Back/exit dialog
logic in `OwnTVShell.kt`. Labels come from core's existing string resources (`MainSection.labelRes`).

**Done when:** CI green; unit-testable state logic (if any) has a test; the report notes the two or three
focus transitions that must be tried on the TV.

### T4 – Replace OwnTV branding with Aura TV (logos, icons, names)

**Wanted:** every logo and icon becomes Aura TV, and every mention of "OwnTV" in the UI becomes "Aura TV".
Seen so far: the exit dialog says "Avsluta OwnTV?", and the default text "OwnTV User" exists in core
strings.

**Approach:**
- Strings live in core (`values*/strings.xml` in core and player-core at `CORE_SHA`; the clone of
  ahXN00/OwnTV_Core is read-only). Write `tools/brand/rebrand_strings.py` that reads those files and emits
  override files `app/src/main/res/values*/aura_brand_strings.xml` for every locale folder that contains a
  string with "OwnTV" in its text, replacing the product name with "Aura TV" (and only the product name:
  never URLs, package names, repo names, file names such as `.own`, or attribution sentences such as "based
  on OwnTV"). Keep `translatable="false"` handling consistent with the core entry. Commit the generated
  output like the other generated brand assets, and make the script deterministic.
  Check that lint's MissingTranslation/ExtraTranslation and `verifyI18nLiterals` stay green.
- Logos and icons: the launcher icon, banner and splash already override core by name. Find the rest
  (grep the manifest and `drawable*`/`mipmap*` names from core: `owntv_icon_*` colour variants and activity
  aliases, `owntv_banner_*`, `owntv_splash_*`, the in-app `BrandLockup`/wordmark drawables, the notification
  icon, the TV channel/launcher recommendation icons) and override each by name with Aura assets generated
  by `tools/brand/render_aura.py` (extend the script; never hand-edit generated files; no face in any icon).
  Where a variant exists per colour theme, give every variant the same Aura mark.
- What stays: README/About credit to OwnTV (GPLv3 attribution), `LICENSE`, namespace `tv.own.owntv`, and
  links that must keep pointing upstream for credit. Telegram/community links that point to OwnTV's own
  channels: hide or replace only if it is a pure UI-entry change; otherwise leave and list in the report.
- Finish with a grep of the built resources list (the `values*` overrides plus a core-string scan) and put
  the remaining "OwnTV" occurrences, with the reason each stays, in the report.

**Done when:** CI green; the script is documented in CLAUDE.md in one line; the report lists leftovers.

## Order of work

Use Johan's order: **T1, T2, T3, T4**. Inside a task, split into steps that each leave `main` green (for
example T4: strings script, string overrides, icon variants, banner and wordmark, leftover scan). Write the
steps into `PROGRESS.md` before starting the task.
