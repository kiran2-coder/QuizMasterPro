package com.quizmaster.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityManageUsersBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity {
    private ActivityManageUsersBinding binding;
    private FirebaseFirestore db;
    private List<UserModel> allUsers;
    private AdminUserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Users");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        allUsers = new ArrayList<>();

        setupRecyclerView();
        loadUsers();
    }

    private void setupRecyclerView() {
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(allUsers, this::manageUser);
        binding.rvUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        UserModel user = doc.toObject(UserModel.class);
                        if (user != null) {
                            user.id = doc.getId();
                            // Ensure name fallback logic if name field is missing
                            if (user.name == null) user.name = user.username;
                            allUsers.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    // Reuse your existing manageUser, toggleStatus, and toggleRole methods here...
    private void manageUser(UserModel user) {
        String[] actions = {
                "restricted".equals(user.status) ? "✅ UNRESTRICT" : "🚫 RESTRICT",
                "quizmaster".equals(user.role) ? "👤 DEMOTE" : "👨‍🏫 PROMOTE",
                "📊 Details"
        };

        new AlertDialog.Builder(this)
                .setTitle("Manage " + user.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) toggleStatus(user);
                    else if (which == 1) toggleRole(user);
                    else showUserDetails(user);
                })
                .show();
    }

    private void toggleStatus(UserModel user) {
        String newStatus = "restricted".equals(user.status) ? "active" : "restricted";
        db.collection("users").document(user.id).update("status", newStatus)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Status Updated!", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh list
                });
    }

    private void toggleRole(UserModel user) {
        String newRole = "quizmaster".equals(user.role) ? "user" : "quizmaster";
        db.collection("users").document(user.id).update("role", newRole)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Role Updated!", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh list
                });
    }

    private void showUserDetails(UserModel user) {
        String details = String.format("Name: %s\nEmail: %s\nRole: %s\nStatus: %s",
                user.name, user.email, user.role, user.status);
        new AlertDialog.Builder(this).setTitle("User Details").setMessage(details).show();
    }
}