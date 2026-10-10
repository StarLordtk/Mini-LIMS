package com.example.minilims.ui

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.minilims.data.AppDatabase
import com.example.minilims.data.ReportData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Files {
    private const val DB_NAME = "lims.db"
    private fun dir(ctx: Context) = File(ctx.cacheDir, "reports").apply { mkdirs() }
    private fun safetyFile(ctx: Context) = File(ctx.filesDir, "before-restore.db")

    fun backupName(): String =
        "lims-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".db"

    fun hasUndo(ctx: Context) = safetyFile(ctx).exists()

    fun share(ctx: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Copy for sharing. Call repo.checkpoint() first. */
    fun backup(ctx: Context): File {
        val dest = File(dir(ctx), backupName())
        ctx.getDatabasePath(DB_NAME).copyTo(dest, overwrite = true)
        return dest
    }

    /** Save the database to a location the person picked (Storage Access Framework). */
    fun writeBackup(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            ctx.getDatabasePath(DB_NAME).inputStream().use { it.copyTo(out) }
        } != null
    } catch (e: Exception) { false }

    // ---------------- restore ----------------

    /** Copies the picked file, validates it, replaces the live database. Returns an error message or null on success. */
    fun restoreFromUri(ctx: Context, uri: Uri, currentHash: String?): String? {
        val tmp = File(ctx.cacheDir, "restore.tmp")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return "Couldn't open that file."
            input.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        } catch (e: Exception) { return "Couldn't read that file." }
        return restoreFile(ctx, tmp, currentHash)
    }

    /** Puts back the copy that was taken just before the last restore. */
    fun undoRestore(ctx: Context, currentHash: String?): String? {
        val tmp = File(ctx.cacheDir, "restore.tmp")
        try { safetyFile(ctx).copyTo(tmp, overwrite = true) } catch (e: Exception) { return "Nothing to undo." }
        return restoreFile(ctx, tmp, currentHash)
    }

    private fun restoreFile(ctx: Context, src: File, currentHash: String?): String? {
        // 1) must be an SQLite file
        try {
            src.inputStream().use { ins ->
                val head = ByteArray(16)
                val n = ins.read(head)
                if (n < 16 || String(head, Charsets.ISO_8859_1) != "SQLite format 3\u0000") {
                    return "That isn't a MiniLIMS backup file."
                }
            }
        } catch (e: Exception) { return "Couldn't read that file." }

        // 2) must contain our tables and the same schema version
        var sdb: SQLiteDatabase? = null
        try {
            sdb = SQLiteDatabase.openDatabase(src.path, null, SQLiteDatabase.OPEN_READWRITE)
            val tables = sdb.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN " +
                    "('patients','tests','patient_rates','samples','sample_tests')", null
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (tables != 5) return "That isn't a MiniLIMS backup file."
            val hash = try {
                sdb.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null)
                    .use { if (it.moveToFirst()) it.getString(0) else null }
            } catch (e: Exception) { null }
            if (currentHash != null && hash != currentHash) {
                return "This backup comes from a different app version and can't be restored."
            }
        } catch (e: Exception) {
            return "That file is damaged or isn't a backup."
        } finally {
            try { sdb?.close() } catch (e: Exception) { }
        }
        File(src.path + "-wal").delete(); File(src.path + "-shm").delete(); File(src.path + "-journal").delete()

        // 3) keep a safety copy of the current data, then swap the file
        val live = ctx.getDatabasePath(DB_NAME)
        try { if (live.exists()) live.copyTo(safetyFile(ctx), overwrite = true) } catch (e: Exception) { }
        AppDatabase.closeAndReset()
        File(live.path + "-wal").delete(); File(live.path + "-shm").delete(); File(live.path + "-journal").delete()
        return try {
            src.copyTo(live, overwrite = true)
            src.delete()
            null
        } catch (e: Exception) {
            "Restore failed: ${e.message}"
        }
    }

    /** Cold-restarts the app so every screen reads the restored database. */
    fun restartApp(ctx: Context) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: return
        ctx.startActivity(Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }

    // ---------------- PDF report ----------------
    fun reportPdf(ctx: Context, lab: String, d: ReportData): File {
        val doc = PdfDocument()
        val w = 595
        val h = 842
        val m = 40f
        val normal = Paint().apply { textSize = 10.5f; isAntiAlias = true }
        val bold = Paint(normal).apply { isFakeBoldText = true }
        val big = Paint(normal).apply { textSize = 18f; isFakeBoldText = true }
        val rule = Paint().apply { strokeWidth = 0.8f; color = Color.GRAY }
        val df = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        var no = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, no).create())
        var y = m + 18f

        fun newPage() {
            doc.finishPage(page)
            no++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, no).create())
            y = m + 10f
        }
        fun text(s: String, p: Paint = normal) {
            if (y > h - 60) newPage()
            page.canvas.drawText(s, m, y, p); y += 16f
        }
        fun divider() {
            page.canvas.drawLine(m, y - 11f, w - m, y - 11f, rule); y += 4f
        }
        fun row(a: String, b: String, c: String, e: String, f: String, p: Paint) {
            if (y > h - 60) newPage()
            val cv = page.canvas
            cv.drawText(a.take(34), m, y, p)
            cv.drawText(b.take(14), 250f, y, p)
            cv.drawText(c.take(10), 330f, y, p)
            cv.drawText(e.take(18), 390f, y, p)
            cv.drawText(f, w - m - p.measureText(f), y, p)
            y += 16f
        }

        page.canvas.drawText(lab.ifBlank { "Laboratory" }, m, y, big); y += 26f
        text("Patient: ${d.patient.name}" + (d.patient.age?.let { "   Age: $it" } ?: "") +
            (if (d.patient.gender.isNotBlank()) "   Sex: ${d.patient.gender}" else ""), bold)
        if (d.patient.phone.isNotBlank()) text("Phone: ${d.patient.phone}")
        text("Sample: ${d.sample.barcode}      Collected: ${df.format(Date(d.sample.collectedAt))}")
        text("Status: ${d.sample.status}")
        y += 6f; divider()
        row("Test", "Result", "Unit", "Reference", "Rate", bold); divider()
        d.lines.forEach { l -> row(l.name, l.result.ifBlank { "-" }, l.unit, l.refRange, "%.2f".format(l.rateApplied), normal) }
        y += 4f; divider()
        row("", "", "", "Total", "%.2f".format(d.sample.total), bold)
        y += 10f
        text("Generated by MiniLIMS on ${df.format(Date())}")
        doc.finishPage(page)

        val safe = d.sample.barcode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir(ctx), "report-$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
