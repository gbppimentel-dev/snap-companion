# Star-photo specifics — code60

The user requested specific setup instructions for star photographs, in addition
to the optional LenTalk walkthroughs. The feature stays in each Pokémon's
existing Photo Tips reader; it adds no tab or manual progress input.

## Interface

- Step-by-Step cards are tied to the selected Pokémon and one-, two-, three- or
  four-star category. Each card shows its course image, time variant and example
  research level where supported. Different routes remain separate methods.
- Expanded cards contain the known location/route, relevant tools, ordered setup
  steps and a distinct The Shot callout for the moment to capture. Existing
  course-specific instructions use the same reader without invented extra steps.
- The short target in the photo book and Photo Tips uses an authored shot goal
  when one exists, instead of a paragraph mixing several different methods.
  Course cards follow those authored methods. Other category examples remain
  explicitly labelled reference examples.
- A connected visible current course sorts its methods first and can display
  Current Course. This does not mean the pose is currently on screen, unlocked
  or available at the current level. Unknown/background contexts supply no such
  preference. Reference levels are examples, never inferred unlock minima.
- Course Guide closes Photo Tips before opening the internal course reader.
  This also fixes the previous example-card route opening behind the modal.
- The same native paper, icons, star assets, centered close and scrolling reader
  are retained. No source hyperlinks or new spoiler setting are added.

## Coverage and provenance

There are 58 independently phrased additional methods for 57 star categories
across 28 Pokémon, including alternate four-star Vivillon methods in Research
Camp and Outaway Cave. These emphasize the opening course roster and several
later examples, including Flareon, Sharpedo, Cutiefly, Cradily and Espeon.

The reader also exposes the existing 442 course-scoped category instructions.
Together these provide 500 method cards across 499 of the 936 categories.
The other 437 categories retain their existing general hints and reference
examples; a detailed route is not invented for them. All saved stars, scores,
photos and category navigation remain independent of this static guide data.

`tools/data/photo-walkthroughs.json` records source URLs and independently phrased
route/setup/timing facts. `tools/prepare_photo_walkthroughs.py` validates Pokémon,
star count, independent course encounters, course level list, supported tools and
nonempty steps, then generates the link-free `photo-details.js`. The publication
audit checks the generated asset against the source. No source article or
screenshot is bundled.

Five unsupported source level labels are withheld: Caterpie's three-star night
example, the Heracross/Pinsir four-star night battle, and Swanna's three-/four-star
night examples. Their source says Lv.3 while the course exposes only two levels.
The source labels and qualification remain in provenance; the app displays no
level rather than changing them to a guessed Lv.2. Course and category facts are
retained. Demonstrated levels elsewhere remain explicitly labelled Example.

These are reference methods, not new live pose recognition or fresh device
gameplay validation. No LenTalk solution is automatically assigned to a star
category; the independently cited photo-category sources establish the mapping.

## Checks

Model checks validate every new method and the scoped fallbacks, category/subject/
course isolation, current-course ordering without source mutation, withholding
impossible levels and rejection of invalid records. Browser checks cover Thor,
short landscape and narrow portrait layouts. They exercise multiple Vivillon
methods, Grookey's handstand, Scorbunny's three-star kick, an unknown level,
Cradily's distinct three-/four-star setups, an existing one-star setup, exact step
and shot text, tools, category stars, bounded reader areas, compact method heading,
optional expansion and Course Guide routing. Existing UI/map cases are retained.

No native helper, bridge, image queue, save decoder, guidebook, original reference
dataset, emulator setting, rail/calibration data or game input behavior changes.
The UI model gains only a pure reference-method selector. Previews use synthetic
progress; personal photographs and device captures are not repository images.

Final checks passed: 74 Node/model cases, 13 browser acceptance cases, 182
Android/JVM tests, 6 diagnostics, native regressions, release lint, publication
audit and original-package/baseline-signer upgrade validation. Focused Photo Tips
and method tests also passed after tightening the method-heading height. The
packaged UI matches source; native libraries, bridge, map/model/rail/calibration
assets and original reference datasets are byte-identical to code59.

APK: `SnapCompanion-0.5.33-dev.3.apk`, versionCode60. SHA-256:
`aae7bb8b1a0f6bd5785003b0fb84a3132288e381436e61c2b5d54038d610d8cc`.

Installed in place on Thor under the standing direct-push authorization. Android
confirms 0.5.33-dev.3 / code60. Thor remained asleep and the same Eden process
survived. No wake, game input, app launch, emulator restart, setting change, save
operation or data clearing was sent. The update will be used on the next
Companion opening. These reference methods were not exercised through new game
runs during this change; UI/model checks do not prove each gameplay procedure.
