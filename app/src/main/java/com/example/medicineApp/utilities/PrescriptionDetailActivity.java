package com.example.medicineApp.utilities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.medicineApp.Dashboard;
import com.example.medicineApp.R;
import com.example.medicineApp.database.TimeTermStatus;
import com.example.medicineApp.ui.RxViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrescriptionDetailActivity extends AppCompatActivity {
    private RxViewModel viewModel;
    private int prescriptionId;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prescription_details);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(RxViewModel.class);

        // Read prescription ID from Intent
        prescriptionId = getIntent().getIntExtra("uid", -1);
        if (prescriptionId <= 0) {
            finish();
            return;
        }

        // UI components
        TextView titleView             = findViewById(R.id.txtTitle);
        TextView descView              = findViewById(R.id.txtDesc);
        TextView datesView             = findViewById(R.id.txtDates);
        TextView timeTermView          = findViewById(R.id.txtTimeTerm);
        TextView doctorNameView        = findViewById(R.id.txtDoctor);
        TextView doctorLocationView    = findViewById(R.id.txtDoctorLoc);
        TextView lastReceivedView      = findViewById(R.id.txtLast);
        TextView receivedTodayView     = findViewById(R.id.txtToday);

        TextView uidView               = findViewById(R.id.txtUid);
        TextView prescriptionNameView  = findViewById(R.id.txtMedName);

        Button receivedTodayBtn        = findViewById(R.id.btnReceivedToday);
        Button openMapsBtn             = findViewById(R.id.btnOpenMaps);
        Button homeBtn                 = findViewById(R.id.btnHome);

        // Observe and display prescription data
        viewModel.prescription(prescriptionId).observe(this, prescription -> {
            if (prescription == null) return;

            titleView.setText(prescription.short_name == null
                    ? "Prescription details"
                    : prescription.short_name + " details");

            uidView.setText(String.valueOf(prescription.uid));
            prescriptionNameView.setText(prescription.short_name == null ? "-" : prescription.short_name);
            descView.setText(prescription.description == null ? "-" : prescription.description);
            datesView.setText(formatDateFullMonth(prescription.start_date) + " → " + formatDateFullMonth(prescription.end_date));
            timeTermView.setText(TimeTermStatus.labelForId(prescription.time_term_id));            doctorNameView.setText(prescription.doctor_name == null ? "-" : prescription.doctor_name);
            doctorLocationView.setText(prescription.doctor_location == null ? "-" : prescription.doctor_location);
            lastReceivedView.setText(prescription.last_date_received == null ? "-" : prescription.last_date_received);
            receivedTodayView.setText(prescription.has_received_today ? "Yes" : "No");

            openMapsBtn.setEnabled(prescription.doctor_location != null && !prescription.doctor_location.trim().isEmpty());
            openMapsBtn.setOnClickListener(v -> openMaps(prescription.doctor_location));
        });

        // Button: mark as received today
        receivedTodayBtn.setOnClickListener(v ->
                viewModel.receivedToday(prescriptionId, rows ->
                        runOnUiThread(() ->
                                Toast.makeText(this,
                                        "Updated successfully: " + rows,
                                        Toast.LENGTH_SHORT).show()))
        );

        // Button: go back to Dashboard
        homeBtn.setOnClickListener(v -> {
            Intent i = new Intent(PrescriptionDetailActivity.this, Dashboard.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }

    // Launch Google Maps with doctor location
    private void openMaps(String address) {
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }

    private static String formatDateFullMonth(String isoDate) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = parser.parse(isoDate);
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            assert date != null;
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate; // fallback
        }
    }

}
