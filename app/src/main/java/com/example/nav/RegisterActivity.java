package com.example.nav;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nav.databinding.ActivityRegisterBinding;
import com.example.nav.databinding.DialogPrivacyPolicyBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        gson = new Gson();
        loadUsers();

        // Setup study place spinner
        List<String> studyPlaces = new ArrayList<>(Arrays.asList(
                "Хакасский государственный университет",
                "Абаканский колледж экономики и права",
                "Хакасский политехнический колледж",
                "Абаканский медицинский колледж",
                "Другое"
        ));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studyPlaces);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerStudyPlace.setAdapter(adapter);

        binding.spinnerStudyPlace.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                binding.editTextCustomStudyPlace.setVisibility(
                        parent.getItemAtPosition(position).equals("Другое") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup date picker
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        binding.textViewDateOfBirth.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                binding.textViewDateOfBirth.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Privacy policy link
        binding.textViewPrivacyPolicyLink.setOnClickListener(v -> showPrivacyPolicyDialog());

        // Register button
        binding.buttonRegister.setOnClickListener(v -> registerUser());
    }

    private void loadUsers() {
        String usersJson = sharedPreferences.getString("users", "");
        Type userListType = new TypeToken<List<User>>(){}.getType();
        users = usersJson.isEmpty() ? new ArrayList<>() : gson.fromJson(usersJson, userListType);
    }

    private void saveUsers() {
        String usersJson = gson.toJson(users);
        sharedPreferences.edit().putString("users", usersJson).apply();
    }

    private boolean isLoginUnique(String login) {
        return users.stream().noneMatch(u -> u.getLogin().equalsIgnoreCase(login));
    }

    private void showPrivacyPolicyDialog() {
        DialogPrivacyPolicyBinding dialogBinding = DialogPrivacyPolicyBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("Политика обработки персональных данных")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("ОК", null)
                .show();
    }

    private void registerUser() {
        String login = binding.editTextLogin.getText().toString().trim();
        String lastName = binding.editTextLastName.getText().toString().trim();
        String firstName = binding.editTextFirstName.getText().toString().trim();
        String patronymic = binding.editTextPatronymic.getText().toString().trim();
        String dateOfBirth = binding.textViewDateOfBirth.getText().toString().trim();
        String studyPlace = binding.spinnerStudyPlace.getSelectedItem().toString();
        String customStudyPlace = binding.editTextCustomStudyPlace.getText().toString().trim();
        String phone = binding.editTextPhone.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        boolean privacyAgreed = binding.checkBoxPrivacyPolicy.isChecked();

        if (login.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || patronymic.isEmpty() ||
                dateOfBirth.isEmpty() || phone.isEmpty() || password.isEmpty() ||
                (studyPlace.equals("Другое") && customStudyPlace.isEmpty())) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isLoginUnique(login)) {
            Toast.makeText(this, "Логин уже занят", Toast.LENGTH_LONG).show();
            return;
        }

        if (!privacyAgreed) {
            Toast.makeText(this, "Необходимо согласиться с политикой обработки данных", Toast.LENGTH_LONG).show();
            return;
        }

        String finalStudyPlace = studyPlace.equals("Другое") ? customStudyPlace : studyPlace;
        User user = new User(login, firstName, lastName, "fighter", new ArrayList<>());
        user.setPatronymic(patronymic);
        user.setDateOfBirth(dateOfBirth);
        user.setStudyPlace(finalStudyPlace);
        user.setPhone(phone);
        users.add(user);
        saveUsers();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_" + login + "_password", password);
        editor.putString("user_" + login + "_role", "fighter");
        editor.putString("user_" + login + "_first_name", firstName);
        editor.putString("user_" + login + "_last_name", lastName);
        editor.putString("user_" + login + "_patronymic", patronymic);
        editor.putString("user_" + login + "_date_of_birth", dateOfBirth);
        editor.putString("user_" + login + "_study_place", finalStudyPlace);
        editor.putString("user_" + login + "_phone", phone);
        editor.apply();

        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}