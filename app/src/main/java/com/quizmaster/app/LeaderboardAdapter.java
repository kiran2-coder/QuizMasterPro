package com.quizmaster.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<ScoreModel> scores;

    public LeaderboardAdapter(List<ScoreModel> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScoreModel score = scores.get(position);
        
        // Dynamic Rank Display
        String rankStr = String.valueOf(position + 1);
        if (position == 0) rankStr += " 🥇";
        else if (position == 1) rankStr += " 🥈";
        else if (position == 2) rankStr += " 🥉";
        
        holder.tvRank.setText(rankStr);
        holder.tvUsername.setText(score.username != null ? score.username : "Unknown");
        holder.tvScore.setText(score.score + " pts (" + score.percentage + "%)");
        holder.tvCategory.setText(score.category != null ? score.category.toUpperCase() : "GENERAL");
    }

    @Override
    public int getItemCount() {
        return scores != null ? scores.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvUsername, tvScore, tvCategory;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
