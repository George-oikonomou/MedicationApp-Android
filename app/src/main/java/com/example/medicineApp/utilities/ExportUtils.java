package com.example.medicineApp.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.medicineApp.database.TimeTermStatus;
import com.example.medicineApp.database.entity.PrescriptionDrugEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for exporting  prescription lists as TXT or HTML files
 * into the Downloads folder.
 */
public final class ExportUtils {

    private ExportUtils() {}

    // ───────────────────────────────
    // Public API
    // ───────────────────────────────

    /**
     * Export prescriptions as a .txt file into Downloads.
     */
    public static void exportTxt(Context ctx, List<PrescriptionDrugEntity> items) throws Exception {
        String fileName = "active_prescriptions_" + nowStamp() + ".txt";
        String body = buildTxt(items);
        writeToDownloads(ctx, fileName, "text/plain", body);
    }

    /**
     * Export prescriptions as a .html file into Downloads.
     */
    public static void exportHtml(Context ctx, List<PrescriptionDrugEntity> items) throws Exception {
        String fileName = "active_prescriptions_" + nowStamp() + ".html";
        String body = buildHtml(items);
        writeToDownloads(ctx, fileName, "text/html", body);
    }

    // ───────────────────────────────
    // Content Builders
    // ───────────────────────────────

    private static String buildTxt(List<PrescriptionDrugEntity> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Active Prescriptions (")
                .append(nowStamp())
                .append(")\n\n");

        for (PrescriptionDrugEntity d : items) {
            sb.append("#").append(d.uid).append(" · ").append(valueOrDash(d.short_name)).append("\n")
                    .append("  Description : ").append(valueOrDash(d.description)).append("\n")
                    .append("  Dates       : ").append(valueOrDash(d.start_date))
                    .append(" → ")
                    .append(valueOrDash(d.end_date)).append("\n")
                    .append("  Time term   : ").append(TimeTermStatus.labelForId(d.time_term_id)).append("\n")
                    .append("  Doctor      : ").append(valueOrDash(d.doctor_name)).append("\n")
                    .append("  Location    : ").append(valueOrDash(d.doctor_location)).append("\n")
                    .append("  Last taken  : ").append(valueOrDash(d.last_date_received)).append("\n")
                    .append("  Today?      : ").append(d.has_received_today ? "Yes" : "No").append("\n\n");
        }

        return sb.toString();
    }

    private static String buildHtml(List<PrescriptionDrugEntity> items) {
        StringBuilder sb = new StringBuilder();

        // Header & styles (mobile friendly)
        sb.append("<!doctype html><html><head><meta charset='utf-8'>")
                .append("<title>Active Prescriptions</title>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>") // responsive scaling
                .append("<style>")
                .append("body{font-family:sans-serif;padding:16px;background:#fafafa;color:#333;font-size:16px;line-height:1.6}")
                .append("h1{color:#444;font-size:20px;margin-bottom:20px}")
                .append(".card{background:#fff;border:1px solid #ddd;border-radius:12px;padding:16px;margin:16px 0;box-shadow:0 2px 6px rgba(0,0,0,0.05)}")
                .append(".k{display:inline-block;background:#eee;color:#555;padding:6px 12px;border-radius:999px;margin-right:8px;font-weight:600;font-size:14px}")
                .append("strong{color:#222;font-size:17px}")
                .append("div{margin:4px 0}") // spacing between rows
                .append("</style>")
                .append("</head><body>");

        // Title
        sb.append("<h1>Active Prescriptions (")
                .append(nowStamp())
                .append(")</h1>");

        // Prescription cards
        for (PrescriptionDrugEntity d : items) {
            sb.append("<div class='card'>")

                    .append("<div><span class='k'>ID: ").append(d.uid).append("</span>")
                    .append("<strong>").append(escape(valueOrDash(d.short_name))).append("</strong></div>")

                    .append("<div><span class='k'>Dates</span>")
                    .append(escape(valueOrDash(d.start_date))).append(" → ").append(escape(valueOrDash(d.end_date))).append("</div>")

                    .append("<div><span class='k'>Schedule</span>")
                    .append(escape(TimeTermStatus.labelForId(d.time_term_id))).append("</div>")

                    .append("<div><span class='k'>Description</span>")
                    .append(escape(valueOrDash(d.description))).append("</div>")

                    .append("<div><span class='k'>Doctor</span>")
                    .append(escape(valueOrDash(d.doctor_name))).append("</div>")

                    .append("<div><span class='k'>Location</span>")
                    .append(escape(valueOrDash(d.doctor_location))).append("</div>")

                    .append("<div><span class='k'>Last received</span>")
                    .append(escape(valueOrDash(d.last_date_received))).append("</div>")

                    .append("<div><span class='k'>Received today</span>")
                    .append(d.has_received_today ? "Yes" : "No").append("</div>")

                    .append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // ───────────────────────────────
    // File Writing
    // ───────────────────────────────

    private static void writeToDownloads(Context ctx, String fileName, String mime, String content) throws Exception {
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MedicineApp");

            uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        } else {
            // Pre-Android Q fallback
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloads, fileName);
            try (OutputStream os = new FileOutputStream(file)) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            Uri.fromFile(file);
            return;
        }

        if (uri == null) throw new IllegalStateException("Failed to create file");

        try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new IllegalStateException("Cannot open output stream");
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ───────────────────────────────
    // Helpers
    // ───────────────────────────────

    /** Returns "-" if null or empty, otherwise the string. */
    private static String valueOrDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }

    /** Returns current timestamp for file names, e.g. 20250908_163211. */
    private static String nowStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    /** Escapes HTML special characters safely. */
    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
