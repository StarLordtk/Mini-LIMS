# MiniLIMS 0.4
Offline Android LIMS (Kotlin, Jetpack Compose, Material 3, Room).

- Pre-loaded with the Suraha Speciality Laboratory rate charts: 550 tests with patient charge,
  lab charge (our cost), sample type and report day (loaded on first launch)
- Margin (patient charge minus lab charge) shown per test and per sample (internal, not on patient reports)
- Import any other rate list from CSV (Settings > Test list and rates); reload the bundled chart any time
- Per-patient special rates on top of the default patient charge
- Barcode-scanned sample registration with test search and "tubes needed" summary
- Result entry, status tracking, PDF report with charges
- Backup, restore (with undo), dark mode, Material You colours

Rate rule: patient-specific rate if set, otherwise the test's default rate (frozen on each sample).

## Get the APK
A) Android Studio: File > Open this folder > Gradle sync > Build > Build APK(s).
B) GitHub: push the folder, Actions > "Build APK" > Run workflow > download the artifact.

If you installed an earlier build, uninstall it first (database schema changed).
CSV format: header row with Test Name, Patient Charge (or Rate), Lab Charge, Sample Type, Report Day
(optional: Code, Unit, Reference Range). Without a header: name, rate, sample type, report day.
