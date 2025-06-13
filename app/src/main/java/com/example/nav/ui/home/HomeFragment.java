package com.example.nav.ui.home;

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
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nav.R;
import com.example.nav.Squad;
import com.example.nav.SquadAdapter;
import com.example.nav.databinding.DialogEditProfileBinding;
import com.example.nav.databinding.FragmentHomeBinding;
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

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private SharedPreferences sharedPreferences;
    private String loggedInUser;
    private List<Squad> userSquads;
    private SquadAdapter adapter;
    private List<Squad> availableSquads;
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;
    private androidx.activity.result.ActivityResultLauncher<String> pickPhotoLauncher;
    private Gson gson;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        gson = new Gson();
        Log.d("HomeFragment", "Logged in user: " + loggedInUser);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickPhotoLauncher.launch("image/*");
                    } else {
                        Toast.makeText(requireContext(), "Разрешение на доступ к медиа отклонено", Toast.LENGTH_SHORT).show();
                        binding.imageView.setImageResource(R.drawable.ic_default_profile);
                    }
                });

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            String filePath = saveImageToInternalStorage(uri, loggedInUser);
                            binding.imageView.setImageURI(Uri.fromFile(new File(filePath)));
                            sharedPreferences.edit().putString("photo_path_" + loggedInUser, filePath).apply();
                            Log.d("HomeFragment", "Saved photo path for " + loggedInUser + ": " + filePath);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Ошибка при загрузке фото", Toast.LENGTH_SHORT).show();
                            Log.e("HomeFragment", "Error saving photo", e);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Load available squads
        String squadsJson = sharedPreferences.getString("available_squads", "");
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        availableSquads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);

        // Load user squads
        String userSquadsJson = sharedPreferences.getString("user_" + loggedInUser + "_squads", "");
        userSquads = userSquadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(userSquadsJson, squadListType);

        // Setup RecyclerView
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SquadAdapter(requireContext(), userSquads, this::removeSquad);
        binding.recyclerView.setAdapter(adapter);

        // Load saved profile text
        String savedProfileText = sharedPreferences.getString("profile_text_" + loggedInUser, getDefaultProfileText(loggedInUser));
        binding.textView2.setText(savedProfileText);
        Log.d("HomeFragment", "Loaded profile text for " + loggedInUser + ": " + savedProfileText);

        // Load saved photo
        String savedPhotoPath = sharedPreferences.getString("photo_path_" + loggedInUser, null);
        if (savedPhotoPath != null) {
            File imageFile = new File(savedPhotoPath);
            if (imageFile.exists()) {
                binding.imageView.setImageURI(Uri.fromFile(imageFile));
                Log.d("HomeFragment", "Loaded photo for " + loggedInUser + ": " + savedPhotoPath);
            } else {
                binding.imageView.setImageResource(R.drawable.ic_default_profile);
                Log.d("HomeFragment", "Photo file not found for " + loggedInUser);
            }
        } else {
            binding.imageView.setImageResource(R.drawable.ic_default_profile);
            Log.d("HomeFragment", "No photo path saved for " + loggedInUser);
        }

        // Setup button listeners
        binding.button3.setOnClickListener(v -> showSquadSelectionDialog());
        binding.button2.setOnClickListener(v -> {
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    android.Manifest.permission.READ_MEDIA_IMAGES :
                    android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                pickPhotoLauncher.launch("image/*");
            } else {
                requestPermissionLauncher.launch(permission);
            }
        });
        binding.button5.setOnClickListener(v -> showEditStatusDialog());
        binding.imageView.setOnClickListener(v -> showProfileDialog());

        return binding.getRoot();
    }

    private String getDefaultProfileText(String login) {
        switch (login) {
            case "fighter1":
                return "Боец отряда";
            case "commander1":
                return "Командир отряда";
            case "admin1":
                return "Администратор";
            default:
                return "Боец отряда";
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

    private void showEditStatusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать статус");

        final EditText input = new EditText(requireContext());
        String currentText = binding.textView2.getText().toString();
        input.setText(currentText);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newText = input.getText().toString();
            binding.textView2.setText(newText);
            sharedPreferences.edit().putString("profile_text_" + loggedInUser, newText).apply();
            Log.d("HomeFragment", "Saved profile text for " + loggedInUser + ": " + newText);
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Load user data
        dialogBinding.editTextLastName.setText(sharedPreferences.getString("user_" + loggedInUser + "_last_name", ""));
        dialogBinding.editTextFirstName.setText(sharedPreferences.getString("user_" + loggedInUser + "_first_name", ""));
        dialogBinding.editTextPatronymic.setText(sharedPreferences.getString("user_" + loggedInUser + "_patronymic", ""));
        dialogBinding.textViewDateOfBirth.setText(sharedPreferences.getString("user_" + loggedInUser + "_date_of_birth", ""));
        dialogBinding.editTextPhone.setText(sharedPreferences.getString("user_" + loggedInUser + "_phone", ""));
        dialogBinding.editTextEmail.setText(sharedPreferences.getString("user_" + loggedInUser + "_email", ""));
        dialogBinding.editTextSocialLink.setText(sharedPreferences.getString("user_" + loggedInUser + "_social_link", ""));
        dialogBinding.editTextPassportSeries.setText(sharedPreferences.getString("user_" + loggedInUser + "_passport_series", ""));
        dialogBinding.editTextPassportNumber.setText(sharedPreferences.getString("user_" + loggedInUser + "_passport_number", ""));
        dialogBinding.editTextPassportIssuedBy.setText(sharedPreferences.getString("user_" + loggedInUser + "_passport_issued_by", ""));
        dialogBinding.editTextPassportIssuedDate.setText(sharedPreferences.getString("user_" + loggedInUser + "_passport_issued_date", ""));

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
        String currentStudyPlace = sharedPreferences.getString("user_" + loggedInUser + "_study_place", "");
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

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Мой профиль")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    // Save updated profile
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
                            .putString("user_" + loggedInUser + "_last_name", lastName)
                            .putString("user_" + loggedInUser + "_first_name", firstName)
                            .putString("user_" + loggedInUser + "_patronymic", patronymic)
                            .putString("user_" + loggedInUser + "_date_of_birth", dateOfBirth)
                            .putString("user_" + loggedInUser + "_study_place", finalStudyPlace)
                            .putString("user_" + loggedInUser + "_phone", phone)
                            .putString("user_" + loggedInUser + "_email", email)
                            .putString("user_" + loggedInUser + "_social_link", socialLink)
                            .putString("user_" + loggedInUser + "_passport_series", passportSeries)
                            .putString("user_" + loggedInUser + "_passport_number", passportNumber)
                            .putString("user_" + loggedInUser + "_passport_issued_by", passportIssuedBy)
                            .putString("user_" + loggedInUser + "_passport_issued_date", passportIssuedDate)
                            .apply();

                    Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showSquadSelectionDialog() {
        if (availableSquads.isEmpty()) {
            Toast.makeText(requireContext(), "Нет доступных отрядов", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] squadNames = availableSquads.stream().map(Squad::getName).toArray(String[]::new);
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите отряд")
                .setItems(squadNames, (dialog, which) -> {
                    Squad selectedSquad = availableSquads.get(which);
                    if (!userSquads.contains(selectedSquad)) {
                        userSquads.add(selectedSquad);
                        saveSquads();
                        adapter.notifyDataSetChanged();
                        updateSquadText();
                        Toast.makeText(requireContext(), "Вы вступили в отряд: " + selectedSquad.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Вы уже в этом отряде", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void removeSquad(Squad squad) {
        userSquads.remove(squad);
        saveSquads();
        adapter.notifyDataSetChanged();
        updateSquadText();
        Toast.makeText(requireContext(), "Вы вышли из отряда: " + squad.getName(), Toast.LENGTH_SHORT).show();
    }

    private void saveSquads() {
        String squadsJson = gson.toJson(userSquads);
        sharedPreferences.edit().putString("user_" + loggedInUser + "_squads", squadsJson).apply();
    }

    private void updateSquadText() {
        if (userSquads.isEmpty()) {
            String profileText = sharedPreferences.getString("profile_text_" + loggedInUser, getDefaultProfileText(loggedInUser));
            binding.textView2.setText(profileText);
        } else {
            String squadNames = userSquads.stream().map(Squad::getName).reduce((a, b) -> a + ", " + b).orElse("");
            binding.textView2.setText("Боец отрядов: " + squadNames);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}