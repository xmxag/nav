package com.example.nav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nav.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Initialize test users if not already present
        initializeTestUsers();

        // Check if user is already logged in
        if (sharedPreferences.contains("logged_in_user")) {
            startMainActivity();
            return;
        }

        binding.buttonLogin.setOnClickListener(v -> {
            String login = binding.editTextLogin.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter login and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify credentials
            String storedPassword = sharedPreferences.getString("user_" + login + "_password", null);
            String role = sharedPreferences.getString("user_" + login + "_role", null);

            if (storedPassword != null && storedPassword.equals(password)) {
                // Save logged-in user
                sharedPreferences.edit()
                        .putString("logged_in_user", login)
                        .putString("user_role", role)
                        .apply();

                startMainActivity();
            } else {
                Toast.makeText(this, "Invalid login or password", Toast.LENGTH_SHORT).show();
            }
        });

        binding.textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.nav.RegisterActivity.class));
        });
    }

    private void initializeTestUsers() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Fighter
        if (!sharedPreferences.contains("user_fighter1_password")) {
            editor.putString("user_fighter1_password", "pass123");
            editor.putString("user_fighter1_role", "fighter");
            editor.putString("user_fighter1_first_name", "John");
            editor.putString("user_fighter1_last_name", "Doe");
        }

        // Commander
        if (!sharedPreferences.contains("user_commander1_password")) {
            editor.putString("user_commander1_password", "pass123");
            editor.putString("user_commander1_role", "commander");
            editor.putString("user_commander1_first_name", "Jane");
            editor.putString("user_commander1_last_name", "Smith");
        }

        // Admin
        if (!sharedPreferences.contains("user_admin1_password")) {
            editor.putString("user_admin1_password", "pass123");
            editor.putString("user_admin1_role", "admin");
            editor.putString("user_admin1_first_name", "Admin");
            editor.putString("user_admin1_last_name", "User");
        }

        editor.apply();
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}