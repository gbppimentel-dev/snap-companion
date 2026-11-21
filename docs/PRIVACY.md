# privacy

**Everything runs locally.** The manifest removes Internet and network-state permissions. There is no companion login, telemetry service, analytics or automatic upload/updater.

The app reads supported Eden/Snap identity and save metadata, selected Photodex images, supported upper-screen text, Cocoon-recorded playtime passive built-in controller state and Android-reported current battery charge/charging state. The bundled native helper additionally reads validated current-game course, selected level, rail/world and render-camera fields. It opens game memory read-only; no raw pointers, process IDs or reader session tokens enter the map WebView. It stores cached records and copied photographs in the companion's private Android storage. Existing historical/private records are preserved when the current UI stops displaying them.

Captured screen pixels are processed in memory and are not written to disk by the capture service. Saved Photodex image copies are distinct from captured screen frames. Recognition is bundled/offline. A normal Android uninstall removes the app's private storage; installing an update with the same signing key preserves it.

Fixed-purpose firmware helpers provide the supported local access. They do not accept arbitrary UI-provided shell commands, upload user data, edit game saves or write game memory. A retained compatibility backend can emit only four fixed, guarded physical-tool presses; those buttons are absent from the current map UI. The passive controller strip only observes input. Scheduling/startup focus behavior is described in [architecture](ARCHITECTURE.md).

The optional development collector reads standard service diagnostics only when explicitly run. It records counters, opaque sessions/source epochs and available reported performance samples, with no raw OCR, photos or profile/user IDs. It never installs, takes focus, changes settings or sends input. Collection itself has overhead.

Public repository images use invented example progress. User saves, personal photographs, raw device captures/logs, console keysets and signing keys are excluded from the repository and release bundles.
