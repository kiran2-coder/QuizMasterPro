package com.quizmaster.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HighScoreAdapter extends RecyclerView.Adapter<HighScoreAdapter.ViewHolder> {
    private List<ScoreModel> scoreList;

    public HighScoreAdapter(List<ScoreModel> scoreList) {
        this.scoreList = scoreList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure R.layout.item_high_score exists in res/layout/
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_high_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScoreModel model = scoreList.get(position);

        // Check if we are in QuizMaster mode or User mode
        // For QuizMaster, we want to see WHO scored.
        if (model.username != null) {
            holder.tvCategory.setText(model.username); // Using the tvCategory field to show name
        } else {
            holder.tvCategory.setText(model.category);
        }

        holder.tvScore.setText(model.score + " pts (" + model.percentage + "%)");
    }
    @Override
    public int getItemCount() {
        return scoreList != null ? scoreList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvScore = itemView.findViewById(R.id.tvScore);
        }
    }
}