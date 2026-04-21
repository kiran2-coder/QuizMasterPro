package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityAdminDashboardBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.core.view.WindowCompat;


public class AdminDashboardActivity extends AppCompatActivity {
    private ActivityAdminDashboardBinding binding;
    private FirebaseFirestore db;
    private List<UserModel> allUsers;
    private AdminUserAdapter adapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean usersLoaded = false;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private String adminName = "Admin";
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Proper edge-to-edge (Material recommended)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        allUsers = new ArrayList<>();

        drawerLayout = binding.drawerLayout;
        navView = binding.navView;

        setupToolbar();
        setupNavigationHeader();
        setupRecyclerView();
        setupNavigationDrawer();
        checkAdminAccess();
    }


    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle(" Admin Dashboard ");  // 🔥 SHORT
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }


    private void setupRecyclerView() {
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(allUsers, this::manageUser);
        binding.rvUsers.setAdapter(adapter);
    }

    private void setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            if (itemId == R.id.nav_manage_users) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
            } else if (itemId == R.id.nav_my_profile) {
                showProfileDialog();
            } else if (itemId == R.id.nav_change_password) {
                showChangePasswordDialog();
            } else if (itemId == R.id.nav_logout) {
                showLogoutDialog();
            }
            return true;
        });
    }


    private void setupNavigationHeader() {
        try {
            View headerView = navView.getHeaderView(0);
            if (headerView != null) {
                TextView tvAdminName = headerView.findViewById(R.id.tvAdminName);
                TextView tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail);

                // Get current user info
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                if (tvAdminEmail != null) {
                    tvAdminEmail.setText(email != null ? email : "admin@quizmaster.com");
                }

                // We set the name to "Loading..." initially until checkAdminAccess()
                // finishes fetching the real name from Firestore
                if (tvAdminName != null) {
                    tvAdminName.setText(adminName);
                }
            }
        } catch (Exception e) {
            Log.e("ADMIN", "Header setup failed: " + e.getMessage());
        }
    }



    private void toggleUsersList() {
        if (binding.layoutManageUsers.getVisibility() == View.GONE) {
            binding.layoutManageUsers.setVisibility(View.VISIBLE);
            if (!usersLoaded) {
                binding.tvUsersCount.setText("🔄 Loading users...");
                loadUsers();
            }
        } else {
            binding.layoutManageUsers.setVisibility(View.GONE);
        }
    }

    private void checkAdminAccess() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");

                        // 🔥 CAPTURE NAME HERE
                        // Try "name" first, then "username", then fallback to email if both are null
                        adminName = doc.getString("name");
                        if (adminName == null) adminName = doc.getString("username");
                        if (adminName == null) adminName = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                        if ("admin".equals(role)) {
                            // Update navigation header name if it exists
                            View headerView = navView.getHeaderView(0);
                            TextView tvAdminName = headerView.findViewById(R.id.tvAdminName); // Ensure this ID exists in your header XML
                            if (tvAdminName != null) {
                                tvAdminName.setText(adminName);
                            }
                            loadLiveStats();
                        } else {
                            Toast.makeText(this, "❌ Admin access required", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Check failed", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
                });
    }


    private void loadLiveStats() {
        updateStats();
        handler.postDelayed(this::updateStats, 10000);
    }

    private void updateStats() {
        db.collection("users").get()
                .addOnSuccessListener(q -> binding.tvTotalUsers.setText("👥 " + q.size()));

        db.collection("users").whereEqualTo("role", "quizmaster").get()
                .addOnSuccessListener(q -> binding.tvQuizMasters.setText("👨‍🏫 " + q.size()));

        db.collectionGroup("questions").get()
                .addOnSuccessListener(q -> {
                    binding.tvTotalQuestions.setText("❓ " + q.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("ADMIN_STATS", "Question Count Failed: " + e.getMessage());
                    // If you see "FAILED_PRECONDITION", the index is definitely missing
                    binding.tvTotalQuestions.setText("❓ Error");
                });

        db.collection("categories").get()
                .addOnSuccessListener(q -> binding.tvCategories.setText("📂 " + q.size()));
    }

    private void loadUsers() {
        binding.tvUsersCount.setText("⚡ Loading users...");
        db.collection("users").limit(50).get()
                .addOnSuccessListener(querySnapshot -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        UserModel user = new UserModel();
                        user.id = doc.getId();
                        user.username = getString(data, "username", "No Name");
                        user.email = getString(data, "email", "No Email");
                        user.role = getString(data, "role", "user");
                        user.status = getString(data, "status", "active");
                        user.name = getString(data, "name", user.username);
                        user.createdAt = getLong(data, "createdAt", 0L);
                        allUsers.add(user);
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvUsersCount.setText("✅ " + allUsers.size() + " users loaded");
                    usersLoaded = true;
                })
                .addOnFailureListener(e ->
                        binding.tvUsersCount.setText("❌ Error: " + e.getMessage()));
    }

    private String getString(Map<String, Object> data, String key, String def) {
        return data.get(key) != null ? data.get(key).toString() : def;
    }

    private long getLong(Map<String, Object> data, String key, long def) {
        return data.get(key) != null ? ((Long) data.get(key)) : def;
    }

    private void manageUser(UserModel user) {
        String[] actions = {
                user.status.equals("restricted") ? "✅ UNRESTRICT" : "🚫 RESTRICT",
                user.role.equals("quizmaster") ? "👤 DEMOTE" : "👨‍🏫 PROMOTE",
                "📊 Details"
        };

        new AlertDialog.Builder(this)
                .setTitle("👤 Manage " + user.name)
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: toggleStatus(user.id, user.status); break;
                        case 1: toggleRole(user.id, user.role); break;
                        case 2: showUserDetails(user); break;
                    }
                })
                .setNegativeButton("❌ Cancel", null)
                .show();
    }

    private void toggleStatus(String userId, String currentStatus) {
        Map<String, Object> updates = new HashMap<>();
        String newStatus = "restricted".equals(currentStatus) ? "active" : "restricted";
        updates.put("status", newStatus);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "✅ " + newStatus.toUpperCase() + "!", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    updateStats();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleRole(String userId, String currentRole) {
        Map<String, Object> updates = new HashMap<>();
        String newRole = "quizmaster".equals(currentRole) ? "user" : "quizmaster";
        updates.put("role", newRole);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "✅ " + newRole.toUpperCase() + "!", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    updateStats();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showUserDetails(UserModel user) {
        String details = String.format(
                "👤 %s\n📧 %s\n🎭 Role: %s\n📊 Status: %s\n📅 Joined: %s",
                user.name, user.email, user.role, user.status,
                user.createdAt > 0 ?
                        new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(user.createdAt)) : "Unknown"
        );
        new AlertDialog.Builder(this)
                .setTitle("User Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showProfileDialog() {
        // We use 'adminName' which was already loaded during checkAdminAccess()
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        new AlertDialog.Builder(this)
                .setTitle("👑 Your Profile")
                .setMessage("👤 Name: " + (adminName != null ? adminName : "Admin") +
                        "\n📧 Email: " + (email != null ? email : "N/A") +
                        "\n🎭 Role: Admin")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showChangePasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🔐 Change Password")
                .setMessage("Password reset email will be sent to your account.")
                .setPositiveButton("Send Reset Email", (dialog, which) -> {
                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    if (email != null) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnSuccessListener(a ->
                                        Toast.makeText(this, "✅ Reset email sent to " + email, Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🚪 Logout?")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (d, w) -> {
                    // 1. Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // 2. Intent to LoginActivity (or SplashActivity)
                    Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);

                    // 3. Clear the activity stack so they can't go back
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);

                    // 4. Finish current activity
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
