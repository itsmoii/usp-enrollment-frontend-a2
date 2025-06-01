package com.example.uspenrolme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uspenrolme.R;
import com.example.uspenrolme.models.ApplicationsModel;

import java.util.ArrayList;

public class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder> {

    private final ArrayList<ApplicationsModel> applicationList;
    private final OnApplicationClickListener clickListener;

    public interface OnApplicationClickListener {
        void onApplicationClick(ApplicationsModel application);
    }

    public ApplicationsAdapter(ArrayList<ApplicationsModel> applicationList, OnApplicationClickListener clickListener) {
        this.applicationList = applicationList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_application_item, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        ApplicationsModel model = applicationList.get(position);
        holder.idText.setText(String.valueOf(model.getId()));
        holder.dateText.setText(model.getDate());
        holder.typeText.setText(model.getType());
        holder.statusText.setText(model.getStatus());

        holder.itemView.setOnClickListener(v -> clickListener.onApplicationClick(model));
    }

    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        TextView idText, dateText, typeText, statusText;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            idText = itemView.findViewById(R.id.app_row_id);
            dateText = itemView.findViewById(R.id.app_row_date);
            typeText = itemView.findViewById(R.id.app_row_type);
            statusText = itemView.findViewById(R.id.app_row_status);
        }
    }
}
