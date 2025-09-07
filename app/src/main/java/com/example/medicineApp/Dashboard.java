package com.example.medicineApp;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.medicineApp.database.AppDB;
import com.example.medicineApp.database.model.PrescriptionModel;
import com.example.medicineApp.ui.PrescriptionViewModel;
import com.example.medicineApp.utilities.PrescriptionCreate;
import com.example.medicineApp.utilities.PrescriptionExport;
import com.example.medicineApp.utilities.PrescriptionAdapter;
import com.example.medicineApp.utilities.PrescriptionProvider;
import com.example.medicineApp.workers.PrescriptionPeriodicWorker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Dashboard extends AppCompatActivity {

    private PrescriptionViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        setupBackgroundWorker();
        setupViewModel();
        setupRecyclerView();
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDB.get(this).prescriptionDao().dailyRecompute(today);
        });
    }


    private void setupBackgroundWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(PrescriptionPeriodicWorker.class, 1, TimeUnit.DAYS).build();

        WorkManager.getInstance(this)
                   .enqueueUniquePeriodicWork("rx_periodic_worker", ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PrescriptionViewModel.class);
    }

    private void setupRecyclerView() {
        RecyclerView recycler       = findViewById(R.id.recycler);
        PrescriptionAdapter adapter = new PrescriptionAdapter();

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        viewModel.activePrescriptions.observe(this, adapter::submitList);
    }

    private void setupButtons() {
        FloatingActionButton fabAdd    = findViewById(R.id.fab_add);
        FloatingActionButton fabDelete = findViewById(R.id.fab_delete);
        ImageButton btnTestProvider    = findViewById(R.id.btn_test_provider);

        fabAdd.setOnClickListener(v -> PrescriptionCreate.show(this, viewModel));
        fabDelete.setOnClickListener(v -> showDeleteDialog());
        findViewById(R.id.btn_export).setOnClickListener(v -> showExportDialog());

        btnTestProvider.setOnClickListener(v -> {
            try {
                testContentProviderCRUD();
            } catch (Exception e) {
                logError(e);
                toast("ERR: " + e.getMessage());
            }
        });
    }


    private void showDeleteDialog() {
        View root = getLayoutInflater().inflate(R.layout.prescription_delete, null, false);

        EditText inputUid = root.findViewById(R.id.inputUid);
        Button btnDelete = root.findViewById(R.id.btnDelete);
        TextView btnCancel = root.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(true)
                .create();

        btnDelete.setOnClickListener(v -> handleDelete(inputUid, dialog));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleDelete(EditText inputUid, AlertDialog dialog) {
        String input = inputUid.getText() == null ? "" : inputUid.getText().toString().trim();

        if (input.isEmpty()) {
            toast("Enter an ID");
            return;
        }

        try {
            int id = Integer.parseInt(input);
            viewModel.deleteByUid(id, rows -> runOnUiThread(() -> {
                toast(rows > 0 ? "Deleted (" + rows + ")" : "ID not found");
                dialog.dismiss();
            }));
        } catch (NumberFormatException e) {
            toast("Invalid ID");
        }
    }

    private void showExportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("PrescriptionExport active medications")
                .setItems(new CharSequence[]{"PrescriptionExport TXT", "PrescriptionExport HTML"}, (d, which) -> {
                    try {
                        List<PrescriptionModel> list =
                                viewModel.activePrescriptions.getValue() == null
                                        ? Collections.emptyList()
                                        : viewModel.activePrescriptions.getValue();

                        if (list.isEmpty()) {
                            toast("Nothing to export");
                            return;
                        }

                        if (which == 0) PrescriptionExport.exportTxt(this, list);
                        else PrescriptionExport.exportHtml(this, list);

                        toast("Saved to Downloads");
                    } catch (Exception e) {
                        toast("PrescriptionExport failed: " + e.getMessage());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Insert, query, update, and delete a test prescription content provider G */
    private void testContentProviderCRUD() {
        ContentValues values = new ContentValues();
        values.put("short_name", "Test Prescription Med");
        values.put("description", "Test description");
        values.put("start_date", "2025-01-01");
        values.put("end_date", "2025-12-31");
        values.put("time_term_id", 7);
        values.put("doctor_name", "Dr. Test");
        values.put("doctor_location", "Athens");
        values.put("is_active", true);
        values.put("has_received_today", false);

        Uri insertedUri = getContentResolver().insert(PrescriptionProvider.CONTENT_URI, values);
        toast("Inserted: " + insertedUri);

        try (Cursor cursor = getContentResolver().query(PrescriptionProvider.CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder sb = new StringBuilder();
                do {
                    int uid = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("short_name"));
                    sb.append("[").append(uid).append("] ").append(name).append(" | ");
                } while (cursor.moveToNext());

                toast("All: " + sb);

                cursor.moveToFirst();
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));

                ContentValues updateVals = new ContentValues();
                updateVals.put("description", "Updated!");
                Uri itemUri = Uri.withAppendedPath(PrescriptionProvider.CONTENT_URI, String.valueOf(id));
                int updated = getContentResolver().update(itemUri, updateVals, null, null);
                toast("Updated rows: " + updated);

                int deleted = getContentResolver().delete(itemUri, null, null);
                toast("Deleted rows: " + deleted);
            } else {
                toast("No rows in provider");
            }
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void logError(Throwable t) {
        Log.e("Dashboard", "ContentProvider test error", t);
    }
}