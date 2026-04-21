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
        holder.tvRank.setText((position + 1) + " 🥇".substring(0, Math.min(2, position + 1)));
        holder.tvUsername.setText(score.username);
        holder.tvScore.setText(score.score + "/" + score.percentage + "%");
        holder.tvCategory.setText(score.category.toUpperCase());
    }

    @Override
    public int getItemCount() {
        return scores.size();
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
