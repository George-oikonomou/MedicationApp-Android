package com.example.medicineApp.utilities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;

import com.example.medicineApp.R;
import com.example.medicineApp.database.AppDb;
import com.example.medicineApp.database.enums.TimeTermEnum;
import com.example.medicineApp.database.model.TimeTermModel;
import com.example.medicineApp.ui.PrescriptionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrescriptionCreate {

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static void show(@NonNull Context ctx, @NonNull PrescriptionViewModel vm) {
        final View root = LayoutInflater.from(ctx).inflate(R.layout.prescription_create, null, false);

        final EditText name        = root.findViewById(R.id.input_short_name);
        final EditText notes       = root.findViewById(R.id.input_description);
        final EditText prescriber  = root.findViewById(R.id.input_doctor_name);
        final EditText location    = root.findViewById(R.id.input_doctor_location);

        final Spinner  termSpinner = root.findViewById(R.id.spinnerTimeTerm);
        final Button   pickStart   = root.findViewById(R.id.btnPickStart);
        final Button   pickEnd     = root.findViewById(R.id.btnPickEnd);
        final TextView txtStart    = root.findViewById(R.id.txtStart);
        final TextView txtEnd      = root.findViewById(R.id.txtEnd);

        final List<Integer> termIds    = new ArrayList<>();
        final List<String>  termLabels = new ArrayList<>();
        final ArrayAdapter<String> termAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, termLabels);
        termSpinner.setAdapter(termAdapter);

        List<TimeTermModel> initial = vm.timeTerms.getValue();

        if (initial == null || initial.isEmpty())
            initial = AppDb.defaultTimeTerms();

        fillTerms(initial, termIds, termLabels, termAdapter);

        if (ctx instanceof LifecycleOwner) {
            vm.timeTerms.observe((LifecycleOwner) ctx, list -> {
                if (list != null && !list.isEmpty()) fillTerms(list, termIds, termLabels, termAdapter);
            });
        }

        final String[] startIso = { todayIso() };
        final String[] endIso   = { todayIso() };
        txtStart.setText(startIso[0]);
        txtEnd.setText(endIso[0]);

        pickStart.setOnClickListener(v -> pickDateIso(ctx, d -> { startIso[0] = d; txtStart.setText(d);} ));
        pickEnd.setOnClickListener(v   -> pickDateIso(ctx, d -> { endIso[0]   = d; txtEnd.setText(d);} ));

        new AlertDialog.Builder(ctx)
                .setTitle("New Prescription")
                .setView(root)
                .setPositiveButton("Save", (d, w) -> {
                    final String title = trim(name.getText());
                    if (title.isEmpty()) {
                        toast(ctx, "Name is required");
                        return;
                    }

                    final int sel = termSpinner.getSelectedItemPosition();

                    if (sel < 0 || sel >= termIds.size()) {
                        toast(ctx, "Choose a schedule");
                        return;
                    }

                    final int termId = termIds.get(sel);

                    if (startIso[0].compareTo(endIso[0]) > 0) {
                        toast(ctx, "End date MUST be after start date");
                        return;
                    }

                    vm.addPrescription(
                            title,
                            trimOrNull(notes.getText()),
                            startIso[0],
                            endIso[0],
                            termId,
                            trimOrNull(prescriber.getText()),
                            trimOrNull(location.getText())
                    );
                    toast(ctx, "Saved");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void pickDateIso(Context ctx, StringTarget cb) {
        final Calendar now = Calendar.getInstance();
        new DatePickerDialog(
                ctx,
                (v, y, m, d) -> cb.set(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private static String todayIso() { return ISO.format(new Date()); }

    private static void fillTerms(List<TimeTermModel> src, List<Integer> ids, List<String> labels, ArrayAdapter<String> ad) {
        ids.clear();
        labels.clear();
        for (TimeTermModel t : src) {
            ids.add(t.id);
            labels.add(TimeTermEnum.labelForId(t.id));
        }
        ad.notifyDataSetChanged();
    }

    private static String trim(CharSequence cs)   { return cs == null ? "" : cs.toString().trim(); }
    private static String trimOrNull(CharSequence cs) { String s = trim(cs); return s.isEmpty() ? null : s; }
    private static void toast(Context c, String m) { Toast.makeText(c, m, Toast.LENGTH_SHORT).show(); }
    private interface StringTarget { void set(String s); }
}