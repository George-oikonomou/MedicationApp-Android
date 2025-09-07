package com.example.medicineApp.database.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.medicineApp.database.enums.TimeTermEnum;

@Entity(tableName = "time_term")
public class TimeTermModel {

    @PrimaryKey
    @NonNull
    public Integer id;

    @NonNull
    public TimeTermEnum name;

    @ColumnInfo(name = "sort_order")
    @NonNull
    public Integer sortOrder;

    public TimeTermModel(@NonNull Integer id, @NonNull TimeTermEnum name, @NonNull Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
    }
}