package com.example.nav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nav.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        binding.buttonRegister.setOnClickListener(v -> {
            String login = binding.editTextLogin.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();
            String firstName = binding.editTextFirstName.getText().toString().trim();
            String lastName = binding.editTextLastName.getText().toString().trim();

            if (login.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if login already exists
            if (sharedPreferences.contains("user_" + login + "_password")) {
                Toast.makeText(this, "Login already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register new user (default role: fighter)
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_" + login + "_password", password);
            editor.putString("user_" + login + "_role", "fighter");
            editor.putString("user_" + login + "_first_name", firstName);
            editor.putString("user_" + login + "_last_name", lastName);
            editor.apply();

            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();

            // Navigate back to LoginActivity
            startActivity(new Intent(this, com.example.nav.LoginActivity.class));
            finish();
        });
    }
}