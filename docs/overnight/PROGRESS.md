# Overnight progress (state branch, CI does not run here)

status: RUNNING
lock_owner: run-2026-10-30T00:32:16Z
heartbeat_utc: 2026-10-30T01:43:52Z
window_end_local: 2026-10-30T06:30+01:00
base_main_sha: 474dd0b

## Steps
- [x] T1 zapping follows the favourites list  DONE 476d265, Android CI run 40980506382 green, i18n run 40980506404 (see log). Not verified on device.
- [x] T2 channel list readability  DONE 2c692ad, Android CI run 40982587903 green. Not verified on device.
- [x] T3 side menu expands over content  DONE efe1f42, Android CI run 40984882799 green (attempt 1 failed i18n literal check, fixed). Not verified on device.
- [ ] T4 Aura TV branding  IN_PROGRESS step 1 (strings script + overrides) cccf1e4 CI run 40986640398; next: icon variants, leftover scan

Status values per step: TODO, IN_PROGRESS (with SHA), DONE (with SHA and CI run id), BLOCKED (with evidence).

## Log
- 2026-10-30 01:3x local: state branch created, runbook at docs/overnight/OVERNIGHT.md on main.

## Morning report
(not written yet)
- 01:52 T1 done and green (476d265). Next: T2.
- 02:13 T2 done and green (2c692ad). Next: T3.
- 02:32 T3 done and green (efe1f42). Next: T4.
