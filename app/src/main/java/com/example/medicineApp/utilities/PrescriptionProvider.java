package com.example.medicineApp.utilities;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.medicineApp.database.AppDb;
import com.example.medicineApp.database.dao.PrescriptionDao;
import com.example.medicineApp.database.model.PrescriptionModel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PrescriptionProvider extends ContentProvider {

    private static final String AUTHORITY   = "com.example.medicineApp.provider";
    private static final String TABLE_NAME  = "prescription_drug";

    public static final Uri CONTENT_URI     = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

    private static final int PRESCRIPTIONS   = 1;
    private static final int PRESCRIPTION_ID = 2;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME, PRESCRIPTIONS);
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME + "/#", PRESCRIPTION_ID);
    }

    private PrescriptionDao prescriptionDAO;

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        if (ctx != null) {
            prescriptionDAO = AppDb.get(ctx).prescriptionDao();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {

        final int match = URI_MATCHER.match(uri);
        AtomicReference<Cursor> cursorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        AppDb.io().execute(() -> {
            try {
                switch (match) {
                    case PRESCRIPTIONS:
                        cursorRef.set(prescriptionDAO.getAllAsCursor());
                        break;
                    case PRESCRIPTION_ID:
                        int id = (int) ContentUris.parseId(uri);
                        cursorRef.set(prescriptionDAO.getByIdAsCursor(id));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown URI in query: " + uri);
                }
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {}
        Cursor cursor = cursorRef.get();

        if (cursor != null) {
            Context ctx = getContext();
            if (ctx != null) {
                cursor.setNotificationUri(ctx.getContentResolver(), uri);
            }
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case PRESCRIPTIONS:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + TABLE_NAME;
            case PRESCRIPTION_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URI in getType: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (URI_MATCHER.match(uri) != PRESCRIPTIONS) {
            throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }
        if (values == null) {
            throw new IllegalArgumentException("ContentValues must not be null");
        }

        PrescriptionModel entity = PrescriptionModel.fromContentValues(values);

        AtomicReference<Long> idRef = new AtomicReference<>(-1L);
        CountDownLatch latch = new CountDownLatch(1);

        AppDb.io().execute(() -> {
            idRef.set(prescriptionDAO.insert(entity));
            latch.countDown();
        });

        try { latch.await(); } catch (InterruptedException ignored) {}
        long id = idRef.get();

        if (id == -1) throw new SQLException("Failed to insert row into: " + uri);

        Uri resultUri = ContentUris.withAppendedId(CONTENT_URI, id);
        notifyChange(resultUri);
        return resultUri;
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {

        if (URI_MATCHER.match(uri) != PRESCRIPTION_ID) {
            throw new IllegalArgumentException("Invalid URI for update: " + uri);
        }
        if (values == null) return 0;

        int id = (int) ContentUris.parseId(uri);
        AtomicInteger rows = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        AppDb.io().execute(() -> {
            rows.set(prescriptionDAO.updateFromContentValues(id, values));
            latch.countDown();
        });

        try { latch.await(); } catch (InterruptedException ignored) {}
        if (rows.get() > 0) notifyChange(uri);
        return rows.get();
    }

    @Override
    public int delete(
            @NonNull Uri uri,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {

        if (URI_MATCHER.match(uri) != PRESCRIPTION_ID) {
            throw new IllegalArgumentException("Invalid URI for delete: " + uri);
        }

        int id = (int) ContentUris.parseId(uri);
        AtomicInteger rows = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        AppDb.io().execute(() -> {
            rows.set(prescriptionDAO.deleteById(id));
            latch.countDown();
        });

        try { latch.await(); } catch (InterruptedException ignored) {}
        if (rows.get() > 0) notifyChange(uri);
        return rows.get();
    }

    private void notifyChange(@NonNull Uri uri) {
        Context ctx = getContext();
        if (ctx != null) {
            ctx.getContentResolver().notifyChange(uri, null);
        }
    }
}
