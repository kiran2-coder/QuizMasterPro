package com.quizmaster.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {
    private List<UserModel> users;
    private UserActionListener listener;

    public interface UserActionListener {
        void onUserAction(UserModel user);
    }

    public AdminUserAdapter(List<UserModel> users, UserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = users.get(position);

        // 1. Safe Name & Email (fallback to No Name/Email if null)
        holder.tvUsername.setText(user.name != null ? user.name : "No Name");
        holder.tvEmail.setText(user.email != null ? user.email : "No Email");

        // 2. Safe Role (Prevents the .toUpperCase() crash)
        String roleStr = (user.role != null) ? user.role.toUpperCase() : "USER";
        holder.tvRole.setText("Role: " + roleStr);

        // 3. Safe Status (Prevents the .toUpperCase() crash)
        String statusStr = (user.status != null) ? user.status.toUpperCase() : "ACTIVE";
        holder.tvStatus.setText("Status: " + statusStr);

        // 🔥 COLOR-CODED ROLES (Using equalsIgnoreCase for safety)
        if ("admin".equalsIgnoreCase(user.role)) {
            holder.tvRole.setBackgroundColor(0xFFFFEB3B); // Yellow
        } else if ("quizmaster".equalsIgnoreCase(user.role)) {
            holder.tvRole.setBackgroundColor(0xFF2196F3); // Blue
        } else {
            holder.tvRole.setBackgroundColor(0xFFE0E0E0); // Gray
        }

        // 🔥 COLOR-CODED STATUS
        if ("restricted".equalsIgnoreCase(user.status)) {
            holder.tvStatus.setBackgroundColor(0xFFE57373); // Red
        } else {
            holder.tvStatus.setBackgroundColor(0xFF81C784); // Green
        }

        holder.itemView.setOnClickListener(v -> listener.onUserAction(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail, tvRole, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
