package com.quizmaster.app;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.quizmaster.app.databinding.ActivityAddQuestionsBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddQuestionsActivity extends AppCompatActivity {
    private ActivityAddQuestionsBinding binding;
    private FirebaseFirestore db;
    private String categoryId, categoryName;
    private QuestionsAdapter questionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get category from intent
        categoryId = getIntent().getStringExtra("categoryId");
        categoryName = getIntent().getStringExtra("categoryName");

        binding.toolbar.setTitle(categoryName + " - Add Questions");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        loadQuestions();
        binding.btnAddQuestion.setOnClickListener(v -> showAddQuestionDialog());
    }

    private void setupRecyclerView() {
        questionsAdapter = new QuestionsAdapter(new ArrayList<>(), new QuestionsAdapter.OnQuestionActionListener() {
            @Override
            public void onEdit(QuestionModel question) {
                showEditQuestionDialog(question);
            }

            @Override
            public void onDelete(String questionId) {
                deleteQuestion(questionId);
            }
        });
        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvQuestions.setAdapter(questionsAdapter);
    }

    private void deleteQuestion(String questionId) {
        db.collection("categories").document(categoryId)
                .collection("questions").document(questionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Question deleted! ✅", Toast.LENGTH_SHORT).show();
                    loadQuestions();
                    refreshCategoryCount();  // 🔥 UPDATE COUNT
                });
    }

    private void showEditQuestionDialog(QuestionModel question) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Question");

        final EditText etQuestion = new EditText(this);
        etQuestion.setText(question.question);
        // 🔥 CHANGED: Use CAP_SENTENCES instead of CAP_WORDS for the question
        etQuestion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final EditText etA = new EditText(this);
        etA.setText(question.options.get("A"));
        etA.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etB = new EditText(this);
        etB.setText(question.options.get("B"));
        etB.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etC = new EditText(this);
        etC.setText(question.options.get("C"));
        etC.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etD = new EditText(this);
        etD.setText(question.options.get("D"));
        etD.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etCorrect = new EditText(this);
        etCorrect.setText(question.correctAnswer);
        etCorrect.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS); // Correct answer usually 'A', 'B', etc.

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        layout.addView(etQuestion);
        layout.addView(etA);
        layout.addView(etB);
        layout.addView(etC);
        layout.addView(etD);
        layout.addView(etCorrect);
        builder.setView(layout);

        builder.setPositiveButton("UPDATE", (dialog, which) -> {
            Map<String, String> options = new HashMap<>();
            options.put("A", etA.getText().toString().trim());
            options.put("B", etB.getText().toString().trim());
            options.put("C", etC.getText().toString().trim());
            options.put("D", etD.getText().toString().trim());

            String questionText = etQuestion.getText().toString().trim();
            updateQuestion(question.id, formatSentence(questionText), options, etCorrect.getText().toString().trim());
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void updateQuestion(String questionId, String question, Map<String, String> options, String correctAnswer) {
        QuestionModel updatedQuestion = new QuestionModel(questionId, question, options, correctAnswer);

        db.collection("categories").document(categoryId)
                .collection("questions").document(questionId)
                .set(updatedQuestion)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Question updated! ✅", Toast.LENGTH_SHORT).show();
                    loadQuestions();
                    refreshCategoryCount();  // 🔥 UPDATE COUNT
                });
    }

    private void loadQuestions() {
        db.collection("categories").document(categoryId)
                .collection("questions").get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<QuestionModel> questions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        QuestionModel question = doc.toObject(QuestionModel.class);
                        if (question != null) {
                            question.id = doc.getId();
                            questions.add(question);
                        }
                    }
                    questionsAdapter.updateQuestions(questions);
                    updateQuestionCount(questions.size());
                });
    }

    private void showAddQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Question");

        final EditText etQuestion = new EditText(this);
        etQuestion.setHint("What is 2+2?");
        // 🔥 CHANGED: Use CAP_SENTENCES instead of CAP_WORDS for the question
        etQuestion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final EditText etA = new EditText(this);
        etA.setHint("A) 3");
        etA.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etB = new EditText(this);
        etB.setHint("B) 4");
        etB.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etC = new EditText(this);
        etC.setHint("C) 5");
        etC.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etD = new EditText(this);
        etD.setHint("D) 6");
        etD.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        final EditText etCorrect = new EditText(this);
        etCorrect.setHint("Correct: B");
        etCorrect.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        layout.addView(etQuestion);
        layout.addView(etA);
        layout.addView(etB);
        layout.addView(etC);
        layout.addView(etD);
        layout.addView(etCorrect);
        builder.setView(layout);

        builder.setPositiveButton("ADD", (dialog, which) -> {
            String questionText = etQuestion.getText().toString().trim();
            if (!questionText.isEmpty()) {
                Map<String, String> options = new HashMap<>();
                options.put("A", etA.getText().toString().trim());
                options.put("B", etB.getText().toString().trim());
                options.put("C", etC.getText().toString().trim());
                options.put("D", etD.getText().toString().trim());

                String correct = etCorrect.getText().toString().trim();
                addQuestion(formatSentence(questionText), options, correct);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void addQuestion(String question, Map<String, String> options, String correctAnswer) {
        String questionId = db.collection("categories").document(categoryId)
                .collection("questions").document().getId();

        QuestionModel q = new QuestionModel(questionId, question, options, correctAnswer);

        db.collection("categories").document(categoryId)
                .collection("questions").document(questionId)
                .set(q)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Question added! ✅", Toast.LENGTH_SHORT).show();
                    loadQuestions();
                    refreshCategoryCount();  // 🔥 UPDATE COUNT
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateQuestionCount(int count) {
        if (binding.tvQuestionCount != null) {
            binding.tvQuestionCount.setText(count + " questions");
        }
    }

    // 🔥 INSTANT CATEGORY COUNT UPDATE
    private void refreshCategoryCount() {
        db.collection("categories").document(categoryId)
                .collection("questions").get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    db.collection("categories").document(categoryId)
                            .update("questionCount", count);
                });
    }

    // Helper to capitalize only the first letter of the string
    private String formatSentence(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
