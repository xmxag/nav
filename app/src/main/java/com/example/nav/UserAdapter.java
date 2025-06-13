package com.example.nav;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nav.R;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private Context context;
    private List<User> users;
    private OnUserClickListener profileListener;
    private OnUserClickListener removeListener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(Context context, List<User> users, OnUserClickListener profileListener, OnUserClickListener removeListener) {
        this.context = context;
        this.users = users;
        this.profileListener = profileListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.textViewUserName.setText(user.getFirstName() + " " + user.getLastName());
        holder.buttonViewProfile.setOnClickListener(v -> profileListener.onUserClick(user));
        holder.buttonRemove.setOnClickListener(v -> removeListener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName;
        Button buttonViewProfile;
        Button buttonRemove;

        UserViewHolder(View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.text_view_user_name);
            buttonViewProfile = itemView.findViewById(R.id.button_view_profile);
            buttonRemove = itemView.findViewById(R.id.button_remove);
        }
    }
}