package com.collage.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.collage.notes.model.Label;

import java.util.List;

public class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.LabelViewHolder> {

    private List<Label> labels;
    private LabelAdapter.OnItemClickListener listener;

    public LabelAdapter(List<Label> labels) {
        this.labels = labels;
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        public TextView label_option;

        public LabelViewHolder(View itemView) {
            super(itemView);
            label_option = itemView.findViewById(R.id.label_option);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Label label);
    }

    public void updateLabels(List<Label> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(LabelAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LabelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.label_item, parent, false);
        return new LabelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabelViewHolder holder, int position) {
        Label label = labels.get(position);
        holder.label_option.setText(label.getName());

        // set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(label);
            }
        });
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }
}
