package com.example.medicineApp.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.medicineApp.database.TimeTermStatus;

@Entity(tableName = "time_term")
public class TimeTermEntity {

    // FIELDS:
    @PrimaryKey
    @NonNull
    public Integer id;

    @NonNull
    public TimeTermStatus name;

    @ColumnInfo(name = "sort_order")
    @NonNull
    public Integer sortOrder;

    // Constructor:
    public TimeTermEntity(@NonNull Integer id, @NonNull TimeTermStatus name, @NonNull Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
    }
}