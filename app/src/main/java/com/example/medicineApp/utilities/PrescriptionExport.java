package com.example.medicineApp.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.medicineApp.database.enums.TimeTermEnum;
import com.example.medicineApp.database.model.PrescriptionModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PrescriptionExport {

    private PrescriptionExport() {}

    public static void exportTxt(Context ctx, List<PrescriptionModel> items) throws Exception {
        String fileName = "active_prescriptions_" + nowStamp() + ".txt";
        String body = buildTxt(items);
        writeToDownloads(ctx, fileName, "text/plain", body);
    }

    public static void exportHtml(Context ctx, List<PrescriptionModel> items) throws Exception {
        String fileName = "active_prescriptions_" + nowStamp() + ".html";
        String body = buildHtml(items);
        writeToDownloads(ctx, fileName, "text/html", body);
    }

    private static String buildTxt(List<PrescriptionModel> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("Active Prescriptions (")
                .append(nowStamp())
                .append(")\n\n");

        for (PrescriptionModel d : items) {
            sb.append("#").append(d.uid).append(" · ").append(valueOrDash(d.short_name)).append("\n")
                    .append("  Description : ").append(valueOrDash(d.description)).append("\n")
                    .append("  Dates       : ").append(valueOrDash(d.start_date))
                    .append(" → ")
                    .append(valueOrDash(d.end_date)).append("\n")
                    .append("  Time term   : ").append(TimeTermEnum.labelForId(d.time_term_id)).append("\n")
                    .append("  Doctor      : ").append(valueOrDash(d.doctor_name)).append("\n")
                    .append("  Location    : ").append(valueOrDash(d.doctor_location)).append("\n")
                    .append("  Last taken  : ").append(valueOrDash(d.last_date_received)).append("\n")
                    .append("  Today?      : ").append(d.has_received_today ? "Yes" : "No").append("\n\n");
        }

        return sb.toString();
    }

    private static String buildHtml(List<PrescriptionModel> items) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html><html><head><meta charset='utf-8'>")
                .append("<title>Active Prescriptions</title>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>")
                .append("body{font-family:sans-serif;padding:16px;background:#fafafa;color:#333;font-size:16px;line-height:1.6}")
                .append("h1{color:#444;font-size:20px;margin-bottom:20px}")
                .append(".card{background:#fff;border:1px solid #ddd;border-radius:12px;padding:16px;margin:16px 0;box-shadow:0 2px 6px rgba(0,0,0,0.05)}")
                .append(".k{display:inline-block;background:#eee;color:#555;padding:6px 12px;border-radius:999px;margin-right:8px;font-weight:600;font-size:14px}")
                .append("strong{color:#222;font-size:17px}")
                .append("div{margin:4px 0}")
                .append("</style>")
                .append("</head><body>");

        sb.append("<h1>Active Prescriptions (")
                .append(nowStamp())
                .append(")</h1>");

        for (PrescriptionModel d : items) {
            sb.append("<div class='card'>")

                    .append("<div><span class='k'>ID: ").append(d.uid).append("</span>")
                    .append("<strong>").append(escape(valueOrDash(d.short_name))).append("</strong></div>")

                    .append("<div><span class='k'>Dates</span>")
                    .append(escape(valueOrDash(d.start_date))).append(" → ").append(escape(valueOrDash(d.end_date))).append("</div>")

                    .append("<div><span class='k'>Schedule</span>")
                    .append(escape(TimeTermEnum.labelForId(d.time_term_id))).append("</div>")

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

    private static void writeToDownloads(Context ctx, String fileName, String mime, String content) throws Exception {
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MedicineApp");

            uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
        } else {
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

    private static String valueOrDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }

    private static String nowStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
