package com.example.nav.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nav.LoginActivity;
import com.example.nav.databinding.FragmentHomeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private String loggedInUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        // Получаем логин текущего пользователя
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        Log.d("HomeFragment", "Logged in user: " + loggedInUser);

        // Инициализация лаунчера для запроса разрешений
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickPhotoLauncher.launch("image/*");
                    } else {
                        Toast.makeText(requireContext(), "Разрешение на доступ к медиа отклонено", Toast.LENGTH_SHORT).show();
                        binding.imageView.setImageURI(null);
                    }
                });

        // Инициализация лаунчера для выбора фото
        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            // Копируем изображение во внутреннее хранилище с уникальным именем для пользователя
                            String filePath = saveImageToInternalStorage(uri, loggedInUser);
                            binding.imageView.setImageURI(Uri.fromFile(new File(filePath)));
                            // Сохраняем путь к файлу в SharedPreferences для текущего пользователя
                            sharedPreferences.edit().putString("photo_path_" + loggedInUser, filePath).apply();
                            Log.d("HomeFragment", "Saved photo path for " + loggedInUser + ": " + filePath);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Ошибка при загрузке фото", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Загружаем сохранённый текст профиля для текущего пользователя
        String defaultProfileText = getDefaultProfileText(loggedInUser);
        String savedProfileText = sharedPreferences.getString("profile_text_" + loggedInUser, defaultProfileText);
        homeViewModel.setText(savedProfileText);
        Log.d("HomeFragment", "Loaded profile text for " + loggedInUser + ": " + savedProfileText);

        // Наблюдаем за изменениями текста профиля и сохраняем в SharedPreferences
        homeViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            binding.textView2.setText(text);
            sharedPreferences.edit().putString("profile_text_" + loggedInUser, text != null ? text : defaultProfileText).apply();
            Log.d("HomeFragment", "Saved profile text for " + loggedInUser + ": " + text);
        });

        // Загружаем сохранённое изображение для текущего пользователя
        String savedPhotoPath = sharedPreferences.getString("photo_path_" + loggedInUser, null);
        if (savedPhotoPath != null) {
            File imageFile = new File(savedPhotoPath);
            if (imageFile.exists()) {
                binding.imageView.setImageURI(Uri.fromFile(imageFile));
                Log.d("HomeFragment", "Loaded photo for " + loggedInUser + ": " + savedPhotoPath);
            } else {
                binding.imageView.setImageURI(null);
                Log.d("HomeFragment", "Photo file not found for " + loggedInUser);
            }
        }

        // Обработчик кнопки загрузки фото
        binding.button2.setOnClickListener(v -> {
            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_IMAGES :
                    Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    == PackageManager.PERMISSION_GRANTED) {
                pickPhotoLauncher.launch("image/*");
            } else {
                requestPermissionLauncher.launch(permission);
            }
        });

        // Обработчик кнопки редактирования профиля
        binding.button5.setOnClickListener(v -> showEditProfileDialog());

        // Обработчик кнопки выхода
        binding.button4.setOnClickListener(v -> showLogoutConfirmationDialog());

        return root;
    }

    private String getDefaultProfileText(String login) {
        switch (login) {
            case "fighter1":
                return "Fighter Profile";
            case "commander1":
                return "Commander Profile";
            case "admin1":
                return "Admin Profile";
            default:
                return "Profile Text";
        }
    }

    private String saveImageToInternalStorage(Uri uri, String login) throws Exception {
        // Используем уникальное имя файла для каждого пользователя
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать профиль");

        final EditText input = new EditText(requireContext());
        String currentText = homeViewModel.getText().getValue();
        input.setText(currentText != null ? currentText : "");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newText = input.getText().toString();
            homeViewModel.setText(newText);
            // Сохранение в SharedPreferences выполняется в наблюдателе LiveData
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    // Очищаем данные сессии
                    sharedPreferences.edit()
                            .remove("logged_in_user")
                            .remove("user_role")
                            .apply();
                    Log.d("HomeFragment", "Logged out user: " + loggedInUser);
                    // Переходим на LoginActivity
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}