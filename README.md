# MiniLIMS 0.2
Offline Android LIMS (Kotlin, Jetpack Compose, Material 3, Room).

- Patients, test master list (rate, unit, reference range), per-patient special rates
- Barcode-scanned sample registration, result entry, status tracking
- PDF report with results and charges
- Backup (save to device or share) and Restore from backup (with safety copy + Undo)
- Material You colours on Android 12+, teal theme otherwise, dark mode, edge-to-edge

Rate rule: patient-specific rate if set, otherwise the test's default rate (frozen on each sample).

## Get the APK
A) Android Studio: File > Open this folder > Gradle sync > Build > Build APK(s).
B) GitHub: push the folder, Actions > "Build APK" > Run workflow > download the artifact.

If you installed an earlier build, uninstall it first (database schema changed in 0.1).
Restoring only accepts backups made by this same app version.
