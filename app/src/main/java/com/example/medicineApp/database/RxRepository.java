package com.example.medicineApp.database;

import androidx.lifecycle.LiveData;

import com.example.medicineApp.database.entity.PrescriptionDrugEntity;
import com.example.medicineApp.database.entity.TimeTermEntity;

import java.util.List;

public class RxRepository {
    // FIELDS:
    private final AppDb db;

    // Constructor:
    public RxRepository(AppDb db) {
        this.db = db;
    }

    // METHODS:
    public void addSync(PrescriptionDrugEntity prescription){
        db.prescriptionDao().insert(prescription);
    }
    public LiveData<List<PrescriptionDrugEntity>> observeAllDrugs() {
        return db.prescriptionDao().observeAll();
    }

    public LiveData<List<TimeTermEntity>> observeTimeTerms() {
        return db.timeTermDao().observeAll();
    }

    public int deleteByIdSync(int id) {
        return db.prescriptionDao().deleteById(id);
    }

    public LiveData<PrescriptionDrugEntity> observeDrug(int id) { return db.prescriptionDao().observeById(id); }
    public int markReceivedTodaySync(int id, String today) { return db.prescriptionDao().markReceivedToday(id, today); }
    public void recompute_is_activeSync(String today){ db.prescriptionDao().recompute_is_active(today); }
}