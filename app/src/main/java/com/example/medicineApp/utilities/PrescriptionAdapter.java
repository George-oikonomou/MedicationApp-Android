package com.example.medicineApp.utilities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicineApp.R;
import com.example.medicineApp.database.enums.TimeTermEnum;
import com.example.medicineApp.database.model.PrescriptionModel;

import java.util.Objects;


public class PrescriptionAdapter extends ListAdapter<PrescriptionModel, PrescriptionAdapter.PrescriptionVH> {

    public PrescriptionAdapter() { super(DIFF); setHasStableIds(true); }

    @Override public long getItemId(int position) { return getItem(position).uid; }

    @NonNull @Override
    public PrescriptionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.prescription_line, parent, false);

        return new PrescriptionVH(v);
    }

    @Override public void onBindViewHolder(@NonNull PrescriptionVH holder, int position) { holder.bind(getItem(position)); }

    public static class PrescriptionVH extends RecyclerView.ViewHolder {
        private final TextView idView, titleView, scheduleView, datesView, subtitleView;

        PrescriptionVH(@NonNull View itemView) {
            super(itemView);
            idView       = itemView.findViewById(R.id.idPill);
            titleView    = itemView.findViewById(R.id.title);
            scheduleView = itemView.findViewById(R.id.pillSchedule);
            datesView    = itemView.findViewById(R.id.dates);
            subtitleView = itemView.findViewById(R.id.subtitle);
        }

        @SuppressLint("SetTextI18n")
        void bind(@NonNull PrescriptionModel rx) {
            final Context c = itemView.getContext();
            idView.setText("ID: " + rx.uid);

            String name = isEmpty(rx.short_name) ? "Medication name" : rx.short_name;
            titleView.setText(name);

            scheduleView.setText(TimeTermEnum.labelForId(rx.time_term_id));

            String start = nonNull(rx.start_date), end = nonNull(rx.end_date);
            datesView.setText(start + " → " + end);

            if (subtitleView != null) subtitleView.setText(nonNull(rx.doctor_name));

            itemView.setOnClickListener(v -> {
                Intent i = new Intent(c, PrescriptionDetail.class);
                i.putExtra("uid", rx.uid);
                c.startActivity(i);
            });
        }

        private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
        private static String nonNull(String s) { return s == null ? "" : s; }
    }

    private static final DiffUtil.ItemCallback<PrescriptionModel> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull PrescriptionModel o, @NonNull PrescriptionModel n) { return o.uid == n.uid; }
        @Override public boolean areContentsTheSame(@NonNull PrescriptionModel o, @NonNull PrescriptionModel n) {
            return o.uid == n.uid &&
                    Objects.equals(o.short_name, n.short_name) &&
                    Objects.equals(o.description, n.description) &&
                    Objects.equals(o.start_date, n.start_date) &&
                    Objects.equals(o.end_date, n.end_date) &&
                    o.time_term_id == n.time_term_id &&
                    Objects.equals(o.doctor_name, n.doctor_name) &&
                    Objects.equals(o.doctor_location, n.doctor_location) &&
                    o.is_active == n.is_active &&
                    Objects.equals(o.last_date_received, n.last_date_received) &&
                    o.has_received_today == n.has_received_today;
        }
    };
}
