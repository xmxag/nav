package com.example.nav.ui.comsos;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.nav.Squad;
import com.example.nav.databinding.DialogEditProfileBinding;
import com.example.nav.databinding.FragmentProfileBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SharedPreferences sharedPreferences;
    private String loggedInUser;
    private String targetUser;
    private Gson gson;
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;
    private androidx.activity.result.ActivityResultLauncher<String> pickPhotoLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        gson = new Gson();

        // Initialize photo pickers
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickPhotoLauncher.launch("image/*");
                    } else {
                        Toast.makeText(requireContext(), "Разрешение на доступ к медиа отклонено", Toast.LENGTH_SHORT).show();
                    }
                });

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            String filePath = saveImageToInternalStorage(uri, targetUser);
                            binding.imageViewProfile.setImageURI(Uri.fromFile(new File(filePath)));
                            sharedPreferences.edit().putString("photo_path_" + targetUser, filePath).apply();
                            Log.d("ProfileFragment", "Saved photo path for " + targetUser + ": " + filePath);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Ошибка при загрузке фото", Toast.LENGTH_SHORT).show();
                            Log.e("ProfileFragment", "Error saving photo", e);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        gson = new Gson();

        // Enable back arrow
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get target user from arguments
        if (getArguments() != null) {
            targetUser = getArguments().getString("user_login", loggedInUser);
        } else {
            targetUser = loggedInUser;
        }
        Log.d("ProfileFragment", "Viewing profile for user: " + targetUser);

        // Load and display profile data
        displayProfileData();

        // Show edit button only for own profile
        if (targetUser.equals(loggedInUser)) {
            binding.buttonEditProfile.setVisibility(View.VISIBLE);
            binding.buttonEditProfile.setOnClickListener(v -> showEditProfileDialog());
        } else {
            binding.buttonEditProfile.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    private void displayProfileData() {
        // Load full name
        String lastName = sharedPreferences.getString("user_" + targetUser + "_last_name", "");
        String firstName = sharedPreferences.getString("user_" + targetUser + "_first_name", "");
        String patronymic = sharedPreferences.getString("user_" + targetUser + "_patronymic", "");
        binding.textViewFullName.setText(lastName + " " + firstName + " " + patronymic);

        // Load login
        binding.textViewLogin.setText("Логин: " + targetUser);

        // Load squads
        String squadsJson = sharedPreferences.getString("user_" + targetUser + "_squads", "");
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        List<Squad> squads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
        String squadNames = squads.stream().map(Squad::getName).collect(Collectors.joining(", "));
        binding.textViewSquads.setText("Отряды: " + (squadNames.isEmpty() ? "Нет отрядов" : squadNames));

        // Load role
        String role = sharedPreferences.getString("user_" + targetUser + "_role", "fighter");
        binding.textViewRole.setText("Роль: " + role);

        // Load date of birth and study place
        binding.textViewDateOfBirth.setText("Дата рождения: " + sharedPreferences.getString("user_" + targetUser + "_date_of_birth", ""));
        binding.textViewStudyPlace.setText("Место учёбы: " + sharedPreferences.getString("user_" + targetUser + "_study_place", ""));

        // Load profile photo
        String savedPhotoPath = sharedPreferences.getString("photo_path_" + targetUser, null);
        Log.d("ProfileFragment", "Photo path for " + targetUser + ": " + savedPhotoPath);
        if (savedPhotoPath != null) {
            File imageFile = new File(savedPhotoPath);
            if (imageFile.exists()) {
                binding.imageViewProfile.setImageURI(Uri.fromFile(imageFile));
                Log.d("ProfileFragment", "Loaded photo for " + targetUser);
            } else {
                binding.imageViewProfile.setImageResource(android.R.drawable.ic_menu_edit);
                Log.w("ProfileFragment", "Photo file not found for " + targetUser);
            }
        } else {
            binding.imageViewProfile.setImageResource(android.R.drawable.ic_menu_edit);
            Log.d("ProfileFragment", "No photo path saved for " + targetUser);
        }

        // Show sensitive data only for own profile or commanders
        String userRole = sharedPreferences.getString("user_" + loggedInUser + "_role", "fighter");
        if (targetUser.equals(loggedInUser) || userRole.equals("commander")) {
            binding.textViewPhone.setText("Телефон: " + sharedPreferences.getString("user_" + targetUser + "_phone", ""));
            binding.textViewEmail.setText("Email: " + sharedPreferences.getString("user_" + targetUser + "_email", ""));
            binding.textViewSocialLink.setText("Соцсеть: " + sharedPreferences.getString("user_" + targetUser + "_social_link", ""));
            String passport = String.format("Паспорт: %s %s, выдан %s, %s",
                    sharedPreferences.getString("user_" + targetUser + "_passport_series", ""),
                    sharedPreferences.getString("user_" + targetUser + "_passport_number", ""),
                    sharedPreferences.getString("user_" + targetUser + "_passport_issued_by", ""),
                    sharedPreferences.getString("user_" + targetUser + "_passport_issued_date", ""));
            binding.textViewPassport.setText(passport);
        } else {
            binding.textViewPhone.setText("Телефон: Скрыто");
            binding.textViewEmail.setText("Email: Скрыто");
            binding.textViewSocialLink.setText("Соцсеть: Скрыто");
            binding.textViewPassport.setText("Паспорт: Скрыто");
        }
    }

    private String saveImageToInternalStorage(Uri uri, String login) throws Exception {
        File file = new File(requireContext().getFilesDir(), "profile_photo_" + login + ".jpg");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return file.getAbsolutePath();
    }

    private void showEditProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Load current data
        dialogBinding.editTextLastName.setText(sharedPreferences.getString("user_" + targetUser + "_last_name", ""));
        dialogBinding.editTextFirstName.setText(sharedPreferences.getString("user_" + targetUser + "_first_name", ""));
        dialogBinding.editTextPatronymic.setText(sharedPreferences.getString("user_" + targetUser + "_patronymic", ""));
        dialogBinding.textViewDateOfBirth.setText(sharedPreferences.getString("user_" + targetUser + "_date_of_birth", ""));
        dialogBinding.editTextPhone.setText(sharedPreferences.getString("user_" + targetUser + "_phone", ""));
        dialogBinding.editTextEmail.setText(sharedPreferences.getString("user_" + targetUser + "_email", ""));
        dialogBinding.editTextSocialLink.setText(sharedPreferences.getString("user_" + targetUser + "_social_link", ""));
        dialogBinding.editTextPassportSeries.setText(sharedPreferences.getString("user_" + targetUser + "_passport_series", ""));
        dialogBinding.editTextPassportNumber.setText(sharedPreferences.getString("user_" + targetUser + "_passport_number", ""));
        dialogBinding.editTextPassportIssuedBy.setText(sharedPreferences.getString("user_" + targetUser + "_passport_issued_by", ""));
        dialogBinding.editTextPassportIssuedDate.setText(sharedPreferences.getString("user_" + targetUser + "_passport_issued_date", ""));

        // Setup study place spinner
        List<String> studyPlaces = new ArrayList<>(Arrays.asList(
                "Хакасский государственный университет",
                "Абаканский колледж экономики и права",
                "Хакасский политехнический колледж",
                "Абаканский медицинский колледж",
                "Другое"
        ));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, studyPlaces);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerStudyPlace.setAdapter(adapter);
        String currentStudyPlace = sharedPreferences.getString("user_" + targetUser + "_study_place", "");
        int position = studyPlaces.indexOf(currentStudyPlace);
        if (position == -1) {
            dialogBinding.spinnerStudyPlace.setSelection(studyPlaces.indexOf("Другое"));
            dialogBinding.editTextCustomStudyPlace.setText(currentStudyPlace);
            dialogBinding.editTextCustomStudyPlace.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.spinnerStudyPlace.setSelection(position);
        }

        dialogBinding.spinnerStudyPlace.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                dialogBinding.editTextCustomStudyPlace.setVisibility(
                        parent.getItemAtPosition(pos).equals("Другое") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup date pickers
        dialogBinding.textViewDateOfBirth.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.textViewDateOfBirth.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.editTextPassportIssuedDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.editTextPassportIssuedDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Add photo change button
        Button changePhotoButton = new Button(requireContext());
        changePhotoButton.setText("Изменить фото");
        changePhotoButton.setOnClickListener(v -> {
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    android.Manifest.permission.READ_MEDIA_IMAGES :
                    android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                pickPhotoLauncher.launch("image/*");
            } else {
                requestPermissionLauncher.launch(permission);
            }
        });

        // Create dialog layout with photo button
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.addView(changePhotoButton);
        dialogLayout.addView(dialogBinding.getRoot());

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать профиль")
                .setView(dialogLayout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String lastName = dialogBinding.editTextLastName.getText().toString().trim();
                    String firstName = dialogBinding.editTextFirstName.getText().toString().trim();
                    String patronymic = dialogBinding.editTextPatronymic.getText().toString().trim();
                    String dateOfBirth = dialogBinding.textViewDateOfBirth.getText().toString().trim();
                    String studyPlace = dialogBinding.spinnerStudyPlace.getSelectedItem().toString();
                    String customStudyPlace = dialogBinding.editTextCustomStudyPlace.getText().toString().trim();
                    String phone = dialogBinding.editTextPhone.getText().toString().trim();
                    String email = dialogBinding.editTextEmail.getText().toString().trim();
                    String socialLink = dialogBinding.editTextSocialLink.getText().toString().trim();
                    String passportSeries = dialogBinding.editTextPassportSeries.getText().toString().trim();
                    String passportNumber = dialogBinding.editTextPassportNumber.getText().toString().trim();
                    String passportIssuedBy = dialogBinding.editTextPassportIssuedBy.getText().toString().trim();
                    String passportIssuedDate = dialogBinding.editTextPassportIssuedDate.getText().toString().trim();

                    if (lastName.isEmpty() || firstName.isEmpty() || patronymic.isEmpty() || dateOfBirth.isEmpty() ||
                            phone.isEmpty() || (studyPlace.equals("Другое") && customStudyPlace.isEmpty())) {
                        Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String finalStudyPlace = studyPlace.equals("Другое") ? customStudyPlace : studyPlace;
                    sharedPreferences.edit()
                            .putString("user_" + targetUser + "_last_name", lastName)
                            .putString("user_" + targetUser + "_first_name", firstName)
                            .putString("user_" + targetUser + "_patronymic", patronymic)
                            .putString("user_" + targetUser + "_date_of_birth", dateOfBirth)
                            .putString("user_" + targetUser + "_study_place", finalStudyPlace)
                            .putString("user_" + targetUser + "_phone", phone)
                            .putString("user_" + targetUser + "_email", email)
                            .putString("user_" + targetUser + "_social_link", socialLink)
                            .putString("user_" + targetUser + "_passport_series", passportSeries)
                            .putString("user_" + targetUser + "_passport_number", passportNumber)
                            .putString("user_" + targetUser + "_passport_issued_by", passportIssuedBy)
                            .putString("user_" + targetUser + "_passport_issued_date", passportIssuedDate)
                            .apply();

                    Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show();
                    displayProfileData();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}