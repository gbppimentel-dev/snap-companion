# Course-reader recovery — code55

The user reported that course detection and map tracking stayed unavailable after
code54, and asked whether another chat’s file reorganization had broken them.

## Findings

- Source history contains no reader/bridge changes in the intervening UI commits.
  The native reader, bundled bridge, guidebook and map model inside code51,
  code52 and code54 have identical hashes. Installed and running helper library
  locations match. No missing/renamed reader file was found.
- Snap was visibly paused during an expedition while the dashboard showed
  Lental Region. Normal link/capture health was current, with no inference
  failures. This isolates the failure beyond basic game connection.
- An independent, bounded, read-only probe found a valid scene with a valid
  current owner, but the cold search had not finished its far-memory sweep.
  The old scheduler repeatedly rescanned a nearby region and gave the rest only
  a 0.2 ms time slice at 20 Hz. The observed eligible memory was about 2.2 GiB.
  A valid candidate is deliberately withheld until its sweep proves unique.
- The heartbeat writer truncated its live file before replacing its contents.
  The helper can observe the empty interval and stop. Helper restarts were
  observed, but their individual causes were not captured; the write race is a
  separate recovery weakness, not a proven explanation for every restart.

The diagnostic never opened/exported the app’s private memory packet and never
wrote game memory, sent a game input, changed graphics settings or saved a photo.

## Correction

- Publish the heartbeat with AtomicFile so a concurrent reader sees a complete
  old or new heartbeat. A concurrent filesystem regression checks 300 refreshes.
- Complete each discovery half once before restarting the search. Keep exact
  build, mapped-range, object type, ownership/lifetime, ambiguity and repeated
  validation checks. No first-hit or timer-based position is substituted.
- Give discovery a 2 ms soft slice, checked between 64 KiB reads, with a 4 MiB
  per-half byte cap. If no owner is found in the completed pass, wait five
  seconds before another pass.
- Use process_vm_readv for bulk reads of at least 4 KiB; keep existing small-field
  reads and the existing read-only file path as a firmware compatibility
  fallback. Partial reads are rejected. Neither path can write game memory.
- Run the helper at background thread priority. During seeking only, advance
  short discovery slices on a 10 ms cadence. Once ready or idle, return to the
  established 20 Hz cadence; the full sweep stops while an owner remains valid.
- Retain the entire code54 interface, native control artwork and Playtime layout.

## Evidence and limits

An independent cold probe recognized Park Day Lv.3 in 11.7 seconds and returned
100/100 ready samples with the paused game’s 70 photos remaining. This first
measurement used the bulk API for all read sizes; the final version restricts it
to bulk reads and keeps existing field reads. Final timing and installed-app
checks are recorded below after verification.

These are paused-course recovery checks, not all-course, moving-game FPS,
thermal, process-restart or long-session crash acceptance. Existing route
alignment and Forest/Camp completion limits still apply. No historical release
artifact or saved record is replaced by this development update.

The final bulk-only version acquired the same paused course in 11.628 seconds.
It returned 100/100 ready samples; wall-clock native sample timing was median
1.400 ms, p95 10.761 ms and maximum 35.555 ms at background thread priority.
Those timings include scheduler delays and are not game frame-time measurements.
The course remained Day Lv.3, at the same measured distance, with 70 photos left.

Code55 built successfully with 181 Android/JVM tests, 73 Node/model checks,
6 diagnostics, native regressions, release lint, publication audit and original
package/signer upgrade checks. No UI files were changed. APK SHA-256:
`6f5eedd51a42bf02ebd3150c8f8c6e898a4784e4eeeee1fc61434a3860568dbe`.

## Installed-app check

Code55 was installed in place and reopened below the same running Eden process.
The first captured dashboard was still neutral during discovery; by the next
checkpoint (26 seconds into the new helper process) it showed Florio Nature Park,
Day, Lv.3 and 70 photos left. The existing 114 Diamond categories and 29.7 hours
were retained. This is the observed checkpoint, not an exact first-ready latency.

Opening Companion’s map with a lower-screen tap showed the NEO-ONE marker,
independent camera cone and Park Trail9% / current-path course7%. The upper screen
remained in the same Pause Menu with 70 photos. No gameplay button, retry, emulator
restart or power/graphics setting change was used. Moving-position tracking and
additional course transitions were not exercised in this check. The map is left
open below the paused game. Temporary diagnostic device files were removed.
