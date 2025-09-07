package com.example.medicineApp.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.medicineApp.database.dao.PrescriptionDAO;
import com.example.medicineApp.database.dao.TimeTermDao;
import com.example.medicineApp.database.entity.PrescriptionDrugEntity;
import com.example.medicineApp.database.entity.TimeTermEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room database for the application.
 * Holds prescription data and time-term definitions.
 */
@Database(
        entities = {PrescriptionDrugEntity.class, TimeTermEntity.class},
        version = 3,
        exportSchema = true
)
@TypeConverters()
public abstract class AppDb extends RoomDatabase {

    // --- DAOs ---
    public abstract PrescriptionDAO prescriptionDao();
    public abstract TimeTermDao timeTermDao();

    // --- Singleton instance ---
    private static volatile AppDb INSTANCE;

    // Executor for running database operations on a background thread
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Returns the singleton instance of the database, creating it if needed.
     * Uses double-checked locking for thread safety.
     */
    public static AppDb get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDb.class,
                                    "rx.db"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    DB_EXECUTOR.execute(() -> {
                                        AppDb appDb = get(context.getApplicationContext());
                                        appDb.timeTermDao().insertAll(defaultTimeTerms());
                                    });
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    DB_EXECUTOR.execute(() -> {
                                        AppDb appDb = get(context.getApplicationContext());
                                        if (appDb.timeTermDao().countSync() == 0) {
                                            appDb.timeTermDao().insertAll(defaultTimeTerms());
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Provides a background-thread executor for DB operations.
     */
    public static ExecutorService io() {
        return DB_EXECUTOR;
    }

    /**
     * Default time-term entries (e.g., before/after meals).
     * Used when DB is created or when the table is empty.
     */
    public static List<TimeTermEntity> defaultTimeTerms() {
        List<TimeTermEntity> list = new ArrayList<>();
        list.add(new TimeTermEntity(1, TimeTermStatus.BEFORE_BREAKFAST, 1));
        list.add(new TimeTermEntity(2, TimeTermStatus.AT_BREAKFAST, 2));
        list.add(new TimeTermEntity(3, TimeTermStatus.AFTER_BREAKFAST, 3));
        list.add(new TimeTermEntity(4, TimeTermStatus.BEFORE_LUNCH, 4));
        list.add(new TimeTermEntity(5, TimeTermStatus.AT_LUNCH, 5));
        list.add(new TimeTermEntity(6, TimeTermStatus.AFTER_LUNCH, 6));
        list.add(new TimeTermEntity(7, TimeTermStatus.BEFORE_DINNER, 7));
        list.add(new TimeTermEntity(8, TimeTermStatus.AT_DINNER, 8));
        list.add(new TimeTermEntity(9, TimeTermStatus.AFTER_DINNER, 9));
        return list;
    }
}
