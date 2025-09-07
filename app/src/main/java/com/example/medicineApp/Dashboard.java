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

import com.example.medicineApp.database.entity.PrescriptionDrugEntity;
import com.example.medicineApp.ui.RxViewModel;
import com.example.medicineApp.utilities.AddPrescription;
import com.example.medicineApp.utilities.ExportUtils;
import com.example.medicineApp.utilities.PrescriptionAdapter;
import com.example.medicineApp.utilities.RxProvider;
import com.example.medicineApp.workers.RxPeriodicWorker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Dashboard activity: displays active prescriptions,
 * allows adding, deleting, exporting, and testing ContentProvider CRUD operations.
 */
public class Dashboard extends AppCompatActivity {

    private RxViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        setupBackgroundWorker();
        setupViewModel();
        setupRecyclerView();
        setupButtons();
    }

    /** Schedule daily background worker. */
    private void setupBackgroundWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(RxPeriodicWorker.class, 1, TimeUnit.DAYS).build();

        WorkManager.getInstance(this)
                   .enqueueUniquePeriodicWork("rx_periodic_worker", ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    /** Initialize ViewModel. */
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(RxViewModel.class);
    }

    /** Setup RecyclerView and adapter. */
    private void setupRecyclerView() {
        RecyclerView recycler       = findViewById(R.id.recycler);
        PrescriptionAdapter adapter = new PrescriptionAdapter();

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        viewModel.activePrescriptions.observe(this, adapter::submitList);
    }

    /** Setup UI buttons. */
    private void setupButtons() {
        FloatingActionButton fabAdd    = findViewById(R.id.fab_add);
        FloatingActionButton fabDelete = findViewById(R.id.fab_delete);
        ImageButton btnTestProvider    = findViewById(R.id.btn_test_provider);

        fabAdd.setOnClickListener(v -> AddPrescription.show(this, viewModel));
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

    /** Insert, query, update, and delete a test prescription. */
    private void testContentProviderCRUD() {
        // Insert
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

        Uri insertedUri = getContentResolver().insert(RxProvider.CONTENT_URI, values);
        toast("Inserted: " + insertedUri);

        // Query
        try (Cursor cursor = getContentResolver().query(RxProvider.CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                StringBuilder sb = new StringBuilder();
                do {
                    int uid = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("short_name"));
                    sb.append("[").append(uid).append("] ").append(name).append(" | ");
                } while (cursor.moveToNext());

                toast("All: " + sb);

                // Reset to first row (same logic as your working version)
                cursor.moveToFirst();
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));

                // Update
                ContentValues updateVals = new ContentValues();
                updateVals.put("description", "Updated!");
                Uri itemUri = Uri.withAppendedPath(RxProvider.CONTENT_URI, String.valueOf(id));
                int updated = getContentResolver().update(itemUri, updateVals, null, null);
                toast("Updated rows: " + updated);

                // Delete
                int deleted = getContentResolver().delete(itemUri, null, null);
                toast("Deleted rows: " + deleted);
            } else {
                toast("No rows in provider");
            }
        }
    }

    /** Show delete dialog. */
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

    /** Handle delete confirmation. */
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

    /** Export prescriptions to TXT or HTML. */
    private void showExportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Export active medications")
                .setItems(new CharSequence[]{"Export TXT", "Export HTML"}, (d, which) -> {
                    try {
                        List<PrescriptionDrugEntity> list =
                                viewModel.activePrescriptions.getValue() == null
                                        ? Collections.emptyList()
                                        : viewModel.activePrescriptions.getValue();

                        if (list.isEmpty()) {
                            toast("Nothing to export");
                            return;
                        }

                        if (which == 0) ExportUtils.exportTxt(this, list);
                        else ExportUtils.exportHtml(this, list);

                        toast("Saved to Downloads");
                    } catch (Exception e) {
                        toast("Export failed: " + e.getMessage());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Utility: show toast. */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Utility: log errors. */
    private void logError(Throwable t) {
        Log.e("Dashboard", "ContentProvider test error", t);
    }
}