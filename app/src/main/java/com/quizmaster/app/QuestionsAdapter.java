package com.quizmaster.app;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Map;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder> {
    private ArrayList<QuestionModel> questions;
    private OnQuestionActionListener listener;

    public interface OnQuestionActionListener {
        void onEdit(QuestionModel question);
        void onDelete(String questionId);
    }

    public QuestionsAdapter(ArrayList<QuestionModel> questions, OnQuestionActionListener listener) {
        this.questions = questions;
        this.listener = listener;
    }

    public void updateQuestions(ArrayList<QuestionModel> newQuestions) {
        this.questions = newQuestions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final QuestionViewHolder holder, final int position) {
        QuestionModel question = questions.get(position);
        holder.tvQuestion.setText((position + 1) + ". " + question.question);

        // Preview options
        String optionsPreview = "";
        if (question.options != null) {
            for (Map.Entry<String, String> option : question.options.entrySet()) {
                optionsPreview += option.getKey() + ": " + option.getValue() + " | ";
            }
        }
        holder.tvOptions.setText(optionsPreview.length() > 50 ?
                optionsPreview.substring(0, 50) + "..." : optionsPreview);

        // 🔥 TAP = EDIT
        holder.itemView.setOnClickListener(v -> listener.onEdit(question));

        // 🔥 LONG PRESS = CONFIRM DELETE (holder.itemView is accessible here)
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Delete Question")
                    .setMessage("Delete this question?")
                    .setPositiveButton("DELETE", (dialog, which) -> listener.onDelete(question.id))
                    .setNegativeButton("CANCEL", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return questions != null ? questions.size() : 0;
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvOptions;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(android.R.id.text1);
            tvOptions = itemView.findViewById(android.R.id.text2);
        }
    }
}
