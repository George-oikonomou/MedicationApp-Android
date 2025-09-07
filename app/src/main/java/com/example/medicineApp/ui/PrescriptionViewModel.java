package com.example.medicineApp.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.medicineApp.database.AppDb;
import com.example.medicineApp.database.repo.PrescriptionRepository;
import com.example.medicineApp.database.model.PrescriptionModel;
import com.example.medicineApp.database.model.TimeTermModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public class PrescriptionViewModel extends AndroidViewModel {

    private final PrescriptionRepository repo;
    public final LiveData<List<TimeTermModel>> timeTerms;
    public final MediatorLiveData<List<PrescriptionModel>> activePrescriptions = new MediatorLiveData<>();

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public PrescriptionViewModel(@NonNull Application app) {
        super(app);
        repo = new PrescriptionRepository(AppDb.get(app));
        timeTerms = repo.observeTimeTerms();
        LiveData<List<PrescriptionModel>> allPrescriptions = repo.observeAllDrugs();

        AppDb.io().execute(() -> repo.recompute_is_activeSync(todayIso()));

        activePrescriptions.addSource(allPrescriptions,
                list -> recomputeActive(list, timeTerms.getValue()));
        activePrescriptions.addSource(timeTerms,
                terms -> recomputeActive(allPrescriptions.getValue(), terms));
    }

    private void recomputeActive(List<PrescriptionModel> prescriptions,
                                 List<TimeTermModel> terms) {
        if (prescriptions == null || prescriptions.isEmpty()) {
            activePrescriptions.setValue(Collections.emptyList());
            return;
        }

        Map<Integer, Integer> sortOrderMap = new HashMap<>();
        if (terms != null) {
            terms.forEach(t -> sortOrderMap.put(t.id, t.sortOrder));
        }

        String today = todayIso();

        List<PrescriptionModel> validPrescriptions = new ArrayList<>();
        for (PrescriptionModel p : prescriptions) {
            boolean inRange = p.start_date.compareTo(today) <= 0
                    && p.end_date.compareTo(today) >= 0;
            if (inRange) validPrescriptions.add(p);
        }

        validPrescriptions.sort(Comparator.comparingInt(p -> {
            Integer v = sortOrderMap.get(p.time_term_id);
            return v != null ? v : Integer.MAX_VALUE;
        }));

        activePrescriptions.setValue(validPrescriptions);
    }

    public void addPrescription(String name, String description, String startIso, String endIso,
                                int timeTermId, String doctor, String location) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Prescription name required");
        if (endIso.compareTo(startIso) < 0)
            throw new IllegalArgumentException("End date must be after start date");

        AppDb.io().execute(() -> {
            try {
                repo.addSync(new PrescriptionModel(
                        name.trim(),
                        safeTrim(description),
                        startIso,
                        endIso,
                        timeTermId,
                        safeTrim(doctor),
                        safeTrim(location)
                ));
            } catch (Exception e) {
                android.util.Log.e("PrescriptionViewModel", "Insert failed", e);
            }
        });
    }

    public LiveData<PrescriptionModel> prescription(int uid) {
        return repo.observeDrug(uid);
    }

    public void deleteByUid(int uid, IntCallback cb) {
        AppDb.io().execute(() -> {
            int rows = repo.deleteByIdSync(uid);
            if (cb != null) cb.accept(rows);
        });
    }

    public void receivedToday(int uid, IntConsumer cb) {
        AppDb.io().execute(() -> {
            int rows = repo.markReceivedTodaySync(uid, todayIso());
            if (cb != null) cb.accept(rows);
        });
    }

    private static String safeTrim(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static String todayIso() {
        return ISO.format(new Date());
    }

    public interface IntCallback {
        void accept(int v);
    }
}