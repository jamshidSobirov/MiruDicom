package com.jamesmobiledev.dicom.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.ui.DicomDetailsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DicomFileAdapter extends RecyclerView.Adapter<DicomFileAdapter.ViewHolder> {

    private final ArrayList<File> dicomFiles;
    private final Context context;
    private final Set<File> selectedFiles = new HashSet<>();


    private OnItemRemovedListener onItemRemovedListener;

    public DicomFileAdapter(Context context, ArrayList<File> dicomFiles) {
        this.context = context;
        this.dicomFiles = dicomFiles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dicom_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = dicomFiles.get(position);
        holder.textView.setText(file.getName());

        // Set click listener for each item
        holder.cardItem.setOnClickListener(v -> {
            Intent intent = new Intent(context, DicomDetailsActivity.class);
            intent.putExtra("dicomFilePath", file.getAbsolutePath());
            context.startActivity(intent);
        });

        holder.btnRemove.setOnClickListener(view -> {
            boolean result = file.delete();
            if (result) {
                Toast.makeText(context, "File has been deleted successfully", Toast.LENGTH_SHORT).show();
                dicomFiles.remove(position);
                notifyItemChanged(position);
                onItemRemovedListener.onItemRemoved();
            } else {
                Toast.makeText(context, "Couldn't delete the file", Toast.LENGTH_SHORT).show();
            }

        });

    }

    @Override
    public int getItemCount() {
        return dicomFiles.size();
    }

    public Set<File> getSelectedFiles() {
        return selectedFiles;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView btnRemove;

        CardView cardItem;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvFileName);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            cardItem = itemView.findViewById(R.id.cardItem);
        }
    }

    public void setOnItemRemovedListener(OnItemRemovedListener onItemRemovedListener) {
        this.onItemRemovedListener = onItemRemovedListener;
    }


    public interface OnItemRemovedListener {
        void onItemRemoved();
    }
}
