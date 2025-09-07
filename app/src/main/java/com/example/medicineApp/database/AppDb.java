package com.example.medicineApp.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.medicineApp.database.dao.PrescriptionDao;
import com.example.medicineApp.database.dao.TimeTermDao;
import com.example.medicineApp.database.enums.TimeTermEnum;
import com.example.medicineApp.database.model.PrescriptionModel;
import com.example.medicineApp.database.model.TimeTermModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(
        entities = {PrescriptionModel.class, TimeTermModel.class},
        version = 1
)
@TypeConverters()
public abstract class AppDb extends RoomDatabase {

    public abstract PrescriptionDao prescriptionDao();
    public abstract TimeTermDao timeTermDao();

    private static volatile AppDb INSTANCE;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

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

    public static ExecutorService io() {
        return DB_EXECUTOR;
    }

    public static List<TimeTermModel> defaultTimeTerms() {
        List<TimeTermModel> list = new ArrayList<>();
        list.add(new TimeTermModel(1, TimeTermEnum.BEFORE_BREAKFAST, 1));
        list.add(new TimeTermModel(2, TimeTermEnum.AT_BREAKFAST, 2));
        list.add(new TimeTermModel(3, TimeTermEnum.AFTER_BREAKFAST, 3));
        list.add(new TimeTermModel(4, TimeTermEnum.BEFORE_LUNCH, 4));
        list.add(new TimeTermModel(5, TimeTermEnum.AT_LUNCH, 5));
        list.add(new TimeTermModel(6, TimeTermEnum.AFTER_LUNCH, 6));
        list.add(new TimeTermModel(7, TimeTermEnum.BEFORE_DINNER, 7));
        list.add(new TimeTermModel(8, TimeTermEnum.AT_DINNER, 8));
        list.add(new TimeTermModel(9, TimeTermEnum.AFTER_DINNER, 9));
        return list;
    }
}
