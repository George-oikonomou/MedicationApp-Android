package com.example.medicineApp.database.repo;

import androidx.lifecycle.LiveData;

import com.example.medicineApp.database.AppDB;
import com.example.medicineApp.database.model.PrescriptionModel;
import com.example.medicineApp.database.model.TimeTermModel;

import java.util.List;

public class PrescriptionRepository {
    private final AppDB db;

    public PrescriptionRepository(AppDB db) {
        this.db = db;
    }
    public void addSync(PrescriptionModel prescription){
        db.prescriptionDao().insert(prescription);
    }
    public LiveData<List<PrescriptionModel>> observeAllPrescriptions() {
        return db.prescriptionDao().observeAll();
    }

    public LiveData<List<TimeTermModel>> observeTimeTerms() {
        return db.timeTermDao().observeAll();
    }

    public int deleteByIdSync(int id) {
        return db.prescriptionDao().deleteById(id);
    }

    public LiveData<PrescriptionModel> observePrescription(int id) { return db.prescriptionDao().observeById(id); }
    public int markReceivedTodaySync(int id, String today) { return db.prescriptionDao().markReceivedToday(id, today); }
    public void recompute_is_activeSync(String today){ db.prescriptionDao().recompute_is_active(today); }
}