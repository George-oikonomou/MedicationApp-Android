package com.example.medicineApp.database.entity;

import android.content.ContentValues;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// --- prescription Table ------------------------
@Entity(
        tableName = "prescription_drug",
        foreignKeys = @ForeignKey(
                entity = TimeTermEntity.class,
                parentColumns = {"id"},
                childColumns = {"time_term_id"},
                onDelete = ForeignKey.RESTRICT
        ),
        indices = {@Index("time_term_id")}
)

public class PrescriptionDrugEntity {

    // FIELDS:
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "short_name")
    public String short_name;

    @Nullable
    public String description;

    @ColumnInfo(name = "start_date")
    public String start_date;

    @ColumnInfo(name = "end_date")
    public String end_date;

    @ColumnInfo(name = "time_term_id")
    public int time_term_id;

    @ColumnInfo(name = "doctor_name")
    @Nullable
    public String doctor_name;

    @ColumnInfo(name = "doctor_location")
    @Nullable
    public String doctor_location;

    @ColumnInfo(name = "is_active")
    public boolean is_active;

    @ColumnInfo(name = "has_received_today")
    public boolean has_received_today;

    @ColumnInfo(name = "last_date_received")
    @Nullable
    public String last_date_received;

    // ---- Constructors -----------------------------------
    public PrescriptionDrugEntity() {}

    public PrescriptionDrugEntity(String short_name,
                                  @Nullable String description,
                                  String start_date,
                                  String end_date,
                                  int time_term_id,
                                  @Nullable String doctor_name,
                                  @Nullable String doctor_location) {
        this.short_name = short_name;
        this.description = description;
        this.start_date = start_date;
        this.end_date = end_date;
        this.time_term_id = time_term_id;
        this.doctor_name = doctor_name;
        this.doctor_location = doctor_location;
        this.is_active = false;
        this.has_received_today = false;
        this.last_date_received = null;
    }
    // ---- End of Constructors ---------------------------

    // METHODS: Creating a new PrescriptionDrugEntity from ContentValues
    public static PrescriptionDrugEntity fromContentValues(ContentValues values) {
        PrescriptionDrugEntity p = new PrescriptionDrugEntity();

        p.uid                = getInt(values, "uid", p.uid);
        p.short_name         = getString(values, "short_name", p.short_name);
        p.description        = getString(values, "description", p.description);
        p.start_date         = getString(values, "start_date", p.start_date);
        p.end_date           = getString(values, "end_date", p.end_date);
        p.time_term_id       = getInt(values, "time_term_id", p.time_term_id);
        p.doctor_name        = getString(values, "doctor_name", p.doctor_name);
        p.doctor_location    = getString(values, "doctor_location", p.doctor_location);
        p.is_active          = getBool(values, "is_active", p.is_active);
        p.last_date_received = getString(values, "last_date_received", p.last_date_received);
        p.has_received_today = getBool(values, "has_received_today", p.has_received_today);

        return p;
    }

    private static String getString(ContentValues v, String key, String def) {
        return v.containsKey(key) ? v.getAsString(key) : def;
    }

    private static int getInt(ContentValues v, String key, int def) {
        Integer val = v.getAsInteger(key);
        return val != null ? val : def;
    }

    private static boolean getBool(ContentValues v, String key, boolean def) {
        Boolean val = v.getAsBoolean(key);
        return val != null ? val : def;
    }
}