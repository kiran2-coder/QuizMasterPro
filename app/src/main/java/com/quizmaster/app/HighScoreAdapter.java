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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_high_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScoreModel model = scoreList.get(position);

        // Display username and category for global leaderboard
        String displayText = (model.username != null ? model.username : "Unknown") 
                + " [" + (model.category != null ? model.category : "N/A") + "]";
        
        holder.tvCategory.setText(displayText);
        
        // 🔥 FORMAT SCORE: Support float/negative marking display
        String formattedScore = String.format("%.2f", model.score).replaceAll("\\.00$", "");
        holder.tvScore.setText(formattedScore + " pts (" + model.percentage + "%)");
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
