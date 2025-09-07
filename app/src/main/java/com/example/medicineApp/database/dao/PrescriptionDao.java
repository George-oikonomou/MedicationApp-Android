package com.example.medicineApp.database.dao;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.medicineApp.database.model.PrescriptionModel;

import java.util.List;

@Dao
public interface PrescriptionDao {
    @Insert
    long insert(PrescriptionModel entity);

    @Query("DELETE FROM prescription_drug WHERE uid = :id")
    int deleteById(int id);

    @Query("SELECT * FROM prescription_drug ORDER BY uid DESC")
    LiveData<List<PrescriptionModel>> observeAll();

    @Query("SELECT * FROM prescription_drug WHERE uid = :id")
    PrescriptionModel getByIdSync(int id);

    @Query("SELECT * FROM prescription_drug WHERE uid = :id")
    LiveData<PrescriptionModel> observeById(int id);

    @Query("UPDATE prescription_drug SET last_date_received = :today, has_received_today = 1 WHERE uid = :id")
    int markReceivedToday(int id, String today);

    @Query("UPDATE prescription_drug SET is_active = CASE WHEN start_date <= :today AND end_date >= :today THEN 1 ELSE 0 END")
    void recompute_is_active(String today);

    @Update
    void update(List<PrescriptionModel> prescription);

    @Update
    int update(PrescriptionModel prescription);

    @Query("SELECT * FROM prescription_drug")
    Cursor getAllAsCursor();

    @Query("SELECT * FROM prescription_drug WHERE uid = :id")
    Cursor getByIdAsCursor(int id);

    @Query("UPDATE prescription_drug " +
            "SET " +
            "    has_received_today = CASE " +
            "        WHEN last_date_received = :today THEN has_received_today " +
            "        ELSE 0 " +
            "    END, " +
            "    is_active = CASE " +
            "        WHEN :today BETWEEN start_date AND end_date THEN 1 " +
            "        ELSE 0 " +
            "    END")
    void dailyRecompute(String today);




    @Transaction
    default long insertFromContentValues(ContentValues values) {
        PrescriptionModel prescription = PrescriptionModel.fromContentValues(values);
        return insert(prescription);
    }

    @Transaction
    default int updateFromContentValues(int id, ContentValues values) {
        PrescriptionModel prescription = getByIdSync(id);
        if (prescription == null) return 0;

        prescription.short_name        = values.getAsString("short_name")        != null ? values.getAsString("short_name")        : prescription.short_name;
        prescription.description       = values.getAsString("description")       != null ? values.getAsString("description")       : prescription.description;
        prescription.start_date        = values.getAsString("start_date")        != null ? values.getAsString("start_date")        : prescription.start_date;
        prescription.end_date          = values.getAsString("end_date")          != null ? values.getAsString("end_date")          : prescription.end_date;
        prescription.doctor_name       = values.getAsString("doctor_name")       != null ? values.getAsString("doctor_name")       : prescription.doctor_name;
        prescription.doctor_location   = values.getAsString("doctor_location")   != null ? values.getAsString("doctor_location")   : prescription.doctor_location;
        prescription.last_date_received= values.getAsString("last_date_received")!= null ? values.getAsString("last_date_received"): prescription.last_date_received;

        Integer timeTermId = values.getAsInteger("time_term_id");
        if (timeTermId != null) prescription.time_term_id = timeTermId;

        Boolean active = values.getAsBoolean("is_active");
        if (active != null) prescription.is_active = active;

        Boolean receivedToday = values.getAsBoolean("has_received_today");
        if (receivedToday != null) prescription.has_received_today = receivedToday;

        return update(prescription);
    }
}