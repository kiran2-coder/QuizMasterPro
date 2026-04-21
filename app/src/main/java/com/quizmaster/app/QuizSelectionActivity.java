package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityQuizSelectionBinding;
import java.util.ArrayList;
import android.util.Log;

public class QuizSelectionActivity extends AppCompatActivity {
    private ActivityQuizSelectionBinding binding;
    private FirebaseFirestore db;
    private CategoryAdapter categoryAdapter;
    private ArrayList<CategoryModel> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setupRecyclerView();
        loadCategories();
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(CategoryModel category) {
                Intent intent = new Intent(QuizSelectionActivity.this, QuizActivity.class);
                intent.putExtra("categoryId", category.customId);  // "science"
                intent.putExtra("categoryName", category.name);
                intent.putExtra("timeLimit", category.timeLimit);
                startActivity(intent);
            }

            @Override
            public void onCategoryDelete(CategoryModel category) {
                // EMPTY - No delete in PLAYER screen
            }
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setAdminMode(false);  // Players NO delete

    }



    private void loadCategories() {
        db.collection("categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categories.clear();

                    // 🔥 INSTANT LOAD - Show immediately
                    for (DocumentSnapshot doc : querySnapshot) {
                        CategoryModel category = doc.toObject(CategoryModel.class);
                        if (category != null) {
                            category.id = doc.getId();
                            categories.add(category);
                        }
                    }

                    // 🔥 UPDATE UI IMMEDIATELY (0.1s)
                    categoryAdapter.updateCategories(categories);

                    // 🔥 BACKGROUND: Update counts silently
                    updateAllQuestionCounts();
                });
    }

    private void updateAllQuestionCounts() {
        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            CategoryModel category = categories.get(i);

            db.collection("categories").document(category.id)
                    .collection("questions").get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (index < categories.size()) {
                            categories.get(index).questionCount = querySnapshot.size();
                            categoryAdapter.notifyItemChanged(index);  // Only 1 item updates
                        }
                    });
        }
    }

}
