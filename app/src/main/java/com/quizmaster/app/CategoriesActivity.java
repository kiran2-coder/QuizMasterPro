package com.quizmaster.app;

import android.content.Intent;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;  // ✅ ADD THIS
import android.widget.Toast;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.quizmaster.app.databinding.ActivityCategoriesBinding;
import java.util.ArrayList;
import com.google.firebase.firestore.WriteBatch;
import android.text.InputType;
import java.util.HashMap;
import java.util.Map;
import com.quizmaster.app.CategoryModel;

public class CategoriesActivity extends AppCompatActivity {
    private ActivityCategoriesBinding binding;
    private FirebaseFirestore db;
    private CategoryAdapter categoryAdapter;
    private ArrayList<CategoryModel> categories = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        loadCategories();  // 🔥 FIXED: This loads ALL counts

        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(CategoryModel category) {
                Intent intent = new Intent(CategoriesActivity.this, AddQuestionsActivity.class);
                intent.putExtra("categoryId", category.id);
                intent.putExtra("categoryName", category.name);
                startActivity(intent);
            }

            @Override
            public void onCategoryDelete(CategoryModel category) {
                showDeleteCategoryDialog(category);  // 🔥 DELETE DIALOG
            }
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setAdminMode(true);  // Admin sees delete

    }

    private void showDeleteCategoryDialog(CategoryModel category) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Delete '" + category.name + "' and all questions?")
                .setPositiveButton("DELETE", (dialog, which) -> deleteCategory(category.id))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void deleteCategory(String categoryId) {
        // Delete all questions first
        db.collection("categories").document(categoryId)
                .collection("questions").get()
                .addOnSuccessListener(querySnapshot -> {
                    // Batch delete all questions
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(db.collection("categories").document(categoryId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Category deleted! ✅", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            });
                });
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


    // 🔥 FIXED: Proper async count loader
    private void loadQuestionCountForCategory(String categoryId, int index) {
        db.collection("categories").document(categoryId)
                .collection("questions").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (index < categories.size()) {
                        categories.get(index).questionCount = querySnapshot.size();
                        categoryAdapter.notifyItemChanged(index);  // Update specific item
                    }
                });
    }



    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Category");

        // 🔥 NEW: 3 INPUT FIELDS
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // 1. NAME FIELD
        final EditText etName = new EditText(this);
        etName.setHint("Category Name (e.g. Math)");
        layout.addView(etName);

        // 2. DESCRIPTION FIELD (keep existing)
        final EditText etDesc = new EditText(this);
        etDesc.setHint("Description (optional)");
        layout.addView(etDesc);

        // 3. TIME LIMIT FIELD ← NEW!
        final EditText etTimeLimit = new EditText(this);
        etTimeLimit.setHint("Time Limit (minutes)");
        etTimeLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etTimeLimit);

        builder.setView(layout);

        builder.setPositiveButton("CREATE", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String timeStr = etTimeLimit.getText().toString().trim();

            // VALIDATION
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (timeStr.isEmpty()) {
                Toast.makeText(this, "Enter time limit", Toast.LENGTH_SHORT).show();
                return;
            }

            int timeLimit = Integer.parseInt(timeStr);
            if (timeLimit < 1 || timeLimit > 60) {
                Toast.makeText(this, "Time limit 1-60 minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            createCategory(name, desc, timeLimit);  // 🔥 PASS TIME LIMIT
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }


    private void createCategory(String name, String desc, int timeLimit) {
        String customId = name.toLowerCase().replaceAll("[^a-z0-9]", "");

        CategoryModel category = new CategoryModel(name, timeLimit);

        // 🔥 USE customId as DOCUMENT ID!
        db.collection("categories").document(customId)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, name + " created! (" + customId + ")", Toast.LENGTH_SHORT).show();
                    loadCategories();
                });
    }




}
