package com.example.medicineApp.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.medicineApp.database.AppDb;

public class PrescriptionPeriodicWorker extends Worker {
    public PrescriptionPeriodicWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        try {
            AppDb db = AppDb.get(getApplicationContext());
            db.prescriptionDao().dailyRecompute(isoToday());
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static String isoToday() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return fmt.format(new java.util.Date());
    }
}