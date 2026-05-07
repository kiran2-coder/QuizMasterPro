package com.quizmaster.app;

import android.content.Intent;
import android.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        loadCategories();

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
                showDeleteCategoryDialog(category);
            }
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setAdminMode(true);

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
        db.collection("categories").document(categoryId)
                .collection("questions").get()
                .addOnSuccessListener(querySnapshot -> {
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
                    for (DocumentSnapshot doc : querySnapshot) {
                        CategoryModel category = doc.toObject(CategoryModel.class);
                        if (category != null) {
                            category.id = doc.getId();
                            categories.add(category);
                        }
                    }
                    categoryAdapter.updateCategories(categories);
                    // Force update question counts from subcollections in case doc count is wrong
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
                        int actualCount = querySnapshot.size();
                        if (index < categories.size()) {
                            categories.get(index).questionCount = actualCount;
                            categoryAdapter.notifyItemChanged(index);
                        }
                        // Also sync the count back to the category document for future fast loads
                        db.collection("categories").document(category.id)
                                .update("questionCount", actualCount);
                    });
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Category");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        final EditText etName = new EditText(this);
        etName.setHint("Category Name (e.g. Math)");
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        layout.addView(etName);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Description (optional)");
        etDesc.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(etDesc);

        final EditText etTimeLimit = new EditText(this);
        etTimeLimit.setHint("Time Limit (minutes)");
        etTimeLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etTimeLimit);

        final CheckBox cbNegative = new CheckBox(this);
        cbNegative.setText("Enable Negative Marking");
        layout.addView(cbNegative);

        final EditText etNegativeMarks = new EditText(this);
        etNegativeMarks.setHint("Deduction (e.g., 0.25)");
        etNegativeMarks.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etNegativeMarks.setVisibility(android.view.View.GONE);
        layout.addView(etNegativeMarks);

        cbNegative.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etNegativeMarks.setVisibility(isChecked ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        builder.setView(layout);

        builder.setPositiveButton("CREATE", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String timeStr = etTimeLimit.getText().toString().trim();
            boolean isNegative = cbNegative.isChecked();
            String negMarksStr = etNegativeMarks.getText().toString().trim();

            if (name.isEmpty() || timeStr.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int timeLimit = Integer.parseInt(timeStr);
            float negativeMarks = 0;
            if (isNegative && !negMarksStr.isEmpty()) {
                negativeMarks = Float.parseFloat(negMarksStr);
            }

            createCategory(name, desc, timeLimit, isNegative, negativeMarks);
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }


    private void createCategory(String name, String desc, int timeLimit, boolean isNegative, float negativeMarks) {
        String formattedName = capitalizeWords(name);
        String customId = formattedName.toLowerCase().replaceAll("[^a-z0-9]", "");

        db.collection("categories").document(customId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Toast.makeText(this, "Category '" + formattedName + "' already exists!", Toast.LENGTH_SHORT).show();
            } else {
                CategoryModel category = new CategoryModel(formattedName, desc, timeLimit, isNegative, negativeMarks);
                db.collection("categories").document(customId)
                        .set(category)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, formattedName + " created! ✅", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to create category", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
