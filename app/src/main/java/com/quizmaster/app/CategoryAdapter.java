package com.quizmaster.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private ArrayList<CategoryModel> categories;
    private OnCategoryClickListener listener;
    private boolean isAdminMode = false;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryModel category);
        void onCategoryDelete(CategoryModel category);
    }

    public CategoryAdapter(ArrayList<CategoryModel> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void updateCategories(ArrayList<CategoryModel> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    public void setAdminMode(boolean adminMode) {
        this.isAdminMode = adminMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.categories_row_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryModel category = categories.get(position);

        holder.tvName.setText(category.name);
        
        // Show negative marking info if enabled
        String negativeInfo = "";
        if (category.negativeMarking) {
            negativeInfo = " | Neg: -" + category.negativeMarks;
        }

        holder.tvQuestionCount.setText(category.questionCount + " Questions | " +
                category.timeLimit + " min" + negativeInfo);

        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        holder.itemView.setOnLongClickListener(v -> {
            if (isAdminMode) {
                listener.onCategoryDelete(category);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvQuestionCount;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvQuestionCount = itemView.findViewById(R.id.tvQuestionCount);
        }
    }
}
