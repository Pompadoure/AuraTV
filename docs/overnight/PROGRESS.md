# Overnight progress (state branch, CI does not run here)

status: DONE
lock_owner: run-2026-10-30T00:32:16Z
heartbeat_utc: 2026-10-30T02:03:00Z
window_end_local: 2026-10-30T06:30+01:00
base_main_sha: 474dd0b

## Steps
- [x] T1 zapping follows the favourites list  DONE 476d265, Android CI run 40980506382 green, i18n run 40980506404 (see log). Not verified on device.
- [x] T2 channel list readability  DONE 2c692ad, Android CI run 40982587903 green. Not verified on device.
- [x] T3 side menu expands over content  DONE efe1f42, Android CI run 40984882799 green (attempt 1 failed i18n literal check, fixed). Not verified on device.
- [x] T4 Aura TV branding  DONE cccf1e4 (strings) + ed82725 (icons/marks/wordmark), Android CI 40988193249 and i18n green. Not verified on device.

Status values per step: TODO, IN_PROGRESS (with SHA), DONE (with SHA and CI run id), BLOCKED (with evidence).

## Log
- 2026-10-30 01:3x local: state branch created, runbook at docs/overnight/OVERNIGHT.md on main.

## Morning report
**Alla fyra uppgifterna är klara och CI är grön på main (ed82725). Inget är testat på riktig TV och inget är installerat.**
APK: https://github.com/Pompadoure/AuraTV/releases/tag/dev (AuraTV-dev.apk, byggd från ed82725).

- T1 Zappning: orsaken var att Guiden startade kanalen via kanalens egen leverantörskategori och ignorerade det filtrerade rutnätet. Nu följer CH+/CH- Guidens visade lista (Favoriter, egna kategorier, Historik, mapp) i dess ordning med wrap; "Alla kanaler" är oförändrat. Enhetstest med dubbletter (SVT1 HD/FHD) och wrap. Listvyn och helskärm hade redan rätt. Multiview ej undersökt.
- T2 Kanallista: radhöjd 84->96, kanalnamn 22->24, programrad 17->18, radavstånd 8->10 (spelarens överlägg 2->6). Allt i LiveRowMetrics (LiveStage.kt), skalar med layoutinställningen. Cirka 7-8 rader syns på 1080p. Guidens egna rader ändrades inte.
- T3 Sidomeny: funktionen fanns redan som "Menu labels: When focused" (ikoner i vila, etiketter över innehållet, innehållet flyttas inte). Den är nu standard via en engångsmigrering vid första start (Docked + Compact utan widen -> Normal). Har du valt Off innan ser det likadant ut och sätts om en gång; ändra tillbaka i Inställningar > Layout.
- T4 Branding: tools/brand/rebrand_strings.py genererar 1752 strängöverskrivningar i 27 språkmappar (OwnTV -> Aura TV, URL:er och attribution orörda). render_aura.py ger nu alla nio färgvarianter samma Aura-ikon, banner, splash, appmärke, AURA TV-ordmärke, notisikon och tematiserad ikon. Pixel-ikonen ritar inte längre OwnTV:s prickmärke. Inställningen "Accent-colored logo" har ingen synlig effekt längre (Aura-märkets triangel är fast).
- Kvarvarande "OwnTV" (kan inte ändras härifrån, ligger i core-koden): lagringsmappen "OwnTV", User-Agent "OwnTV", kraschrapportens rubrik, enhetsnamn vid lokal synk, mpv-wakelock, felet "Not an OwnTV backup file", loggtaggar, themenamn/klassnamn (Theme.OwnTV, OwnTVApp), README/About-kredit till OwnTV (ska vara kvar), Weblate-/GitHub-länkar och .own-filändelsen.

Behöver ses på TV: (1) Guide filtrerad på Favoriter: CH+/CH- hoppar rätt; (2) kanallistans radhöjd och fokusmarkering; (3) vänster från innehållet öppnar menyn över innehållet, höger/Back stänger den; (4) ikon, banner, splash, ordmärke och Avsluta-dialogen heter Aura TV.
