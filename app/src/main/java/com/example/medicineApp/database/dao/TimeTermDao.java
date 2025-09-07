package com.example.medicineApp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.medicineApp.database.model.TimeTermModel;

import java.util.List;

@Dao
public interface TimeTermDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TimeTermModel> items);

    @Query("SELECT * FROM time_term ORDER BY sort_order ASC")
    LiveData<List<TimeTermModel>> observeAll();

    @Query("SELECT COUNT(*) FROM time_term")
    int countSync();
}