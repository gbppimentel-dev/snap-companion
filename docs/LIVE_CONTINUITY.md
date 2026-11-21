# Live continuity and photo hints — code62

User scope: remove the automatic Photo Selection page, check disappearing route
markers around events/forks, distinguish alternate photo hints, and add quiet
battery health to the header. Deliver as an in-place APK update on Thor.

## Behavior

- Live keeps its usual course/suggestion/recent-photos layout during Photo
  Selection. The automatic desk, comparison actions and cross-tab notice are
  removed. Capture/save synchronization remains active.
- Native observations already validate an owned expedition scene and rail across
  a bounded phase range. The UI now calls those non-gameplay phases `event`
  instead of rejecting their rail position as an unknown transition. This does
  not enable tool input, photo counters or camera cones in event phases. Unknown
  out-of-range phases remain neutral. Native offsets are unchanged. A transient rail/owner consistency failure reports
  seeking rather than an expedition-ended idle state; explicit non-expedition
  scope still reports idle. The existing ownership and reread guards are retained.
- Illustrated-map placement needs a matched rail and measured distance, not an
  additional world-coordinate field that the projection does not use. An absent
  world field is allowed; a present malformed world field is still rejected.
- CourseTracker keeps observed branch history across gaps up to five seconds.
  Longer gaps, run/course changes and rewinds reset it. Ambiguous paths retain
  ranges rather than choosing an unobserved branch.
- A brief missing or unmapped reading can retain the last confirmed marker for
  at most 2.5 seconds. It stays stationary, uses a dashed treatment, hides the
  camera cone and says Last Position / Reconnecting. It is never extrapolated.
  Idle/unknown scene exits, visibility loss, a new source, run or course clear
  the hold. Zoom is retained. A valid new measurement replaces it immediately.
- Camera-cone validity is part of the render signature so a previously missing
  cone can recover even if the position and camera direction are unchanged.
- 258 existing category-hint groups recover 612 options from the cached original
  reference line breaks. Every option word must join back to the existing hint;
  no new photo instructions or guessed OR boundaries are introduced. Vivillon's
  two independent photo sentences are separately preserved. Options use distinct
  cards and OR separators; numbered steps remain inside their option. Reviewed
  course methods remain expandable, with OR between separate methods.
- Header battery: Android-reported charge percentage, charging state and health
  condition, sampled at most once per 15 seconds. Good/Unknown are system health
  labels, not remaining full-charge capacity estimates. No capacity percentage
  is fabricated. Missing charge data hides the indicator.

## Verification and limits

Model checks cover event frames, missing/malformed world data, selected forks and
rejoins after brief gaps, long-gap resets, stationary hold expiry and boundary
clearing. Android tests cover health/charge distinctions and scoped event fields.
Browser checks cover normal Live during selection, header fitting, separate hint
options, map event/hold recovery, retained zoom and source/idle clearing.

Existing unaligned map intervals still remain unmapped. These changes do not
prove every course or event has accurate illustrated-map alignment. Hardware
observations and deployment results are recorded below when completed.

## Build and Thor delivery — 2026-11-21

- Signed development APK: `0.5.35-dev.1` / code62, original package and baseline
  signing certificate. Required build, release lint, 200 Android tests, 79 model/map
  tests, six diagnostic cases and native reader checks passed.
- All 16 browser cases passed. After visual inspection, the option header received
  a compact-style correction; its focused three-size browser check also passed,
  followed by the required build. Packaged UI bytes match local source.
- Final APK SHA-256:
  `a9fbf41849632aa46e03622fe845aeb1c113db950fff07575d2709603586fc03`.
- In-place installation succeeded; package metadata confirms code62. The companion
  is open below Snap with charge/Good health visible. Eden kept the same process
  through installation; no emulator settings, Photodex records or photos were
  changed/submitted. The visible saved collection remained 191 Diamond photos.
- Game navigation reached Snowfields Day/Night course selection. Subsequent safe
  confirmation attempts did not launch an expedition. No live fork/event run was
  completed this turn. Route fixes are source/model/browser verified; actual
  event/fork recognition and existing illustrated alignment are not claimed as
  hardware accepted by this delivery.
