package com.example.nav.ui.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nav.Squad;
import com.example.nav.User;
import com.example.nav.UserAdapter;
import com.example.nav.databinding.DialogSquadBinding;
import com.example.nav.databinding.DialogUsersBinding;
import com.example.nav.databinding.FragmentAdminBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminFragment extends Fragment {

    private FragmentAdminBinding binding;
    private SharedPreferences sharedPreferences;
    private List<Squad> availableSquads;
    private List<String> directions;
    private Gson gson;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminBinding.inflate(inflater, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        gson = new Gson();

        // Load available squads
        loadAvailableSquads();

        // Initialize directions
        directions = availableSquads.stream()
                .map(Squad::getDirection)
                .distinct()
                .collect(Collectors.toList());

        // Setup button listeners
        binding.button7.setOnClickListener(v -> showUsersDialog());
        binding.button8.setOnClickListener(v -> showAssignCommanderDialog());
        binding.button9.setOnClickListener(v -> showAssignCuratorDialog());
        binding.button11.setOnClickListener(v -> showAddSquadDialog());
        binding.button12.setOnClickListener(v -> showDeleteSquadDialog());
        binding.button13.setOnClickListener(v -> showEditSquadDialog());

        return binding.getRoot();
    }

    private void loadAvailableSquads() {
        String squadsJson = sharedPreferences.getString("available_squads", "");
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        if (!squadsJson.isEmpty()) {
            availableSquads = gson.fromJson(squadsJson, squadListType);
        } else {
            availableSquads = new ArrayList<>(Arrays.asList(
                    new Squad("Грачи", "ССО", "Информация о Грачи"),
                    new Squad("Бобры", "ССО", "Информация о Бобры"),
                    new Squad("Алтын", "ССО", "Информация о Алтын"),
                    new Squad("Я Есть Грут", "ССервО", "Информация о Я Есть Грут"),
                    new Squad("Еноты", "ССервО", "Информация о Еноты"),
                    new Squad("Стар Лорд", "ССервО", "Информация о Стар Лорд"),
                    new Squad("Медведи", "СПО", "Информация о Медведи"),
                    new Squad("Ирбис", "СПО", "Информация о Ирбис"),
                    new Squad("Сибирские Волки", "СПО", "Информация о Сибирские Волки"),
                    new Squad("Сердце Хакасии", "СОП", "Информация о Сердце Хакасии"),
                    new Squad("Ниже Нуля", "ОСД", "Информация о Ниже Нуля"),
                    new Squad("Зимняя Вишня", "ОСД", "Информация о Зимняя Вишня")
            ));
            saveAvailableSquads();
        }
    }

    private void saveAvailableSquads() {
        String squadsJson = gson.toJson(availableSquads);
        sharedPreferences.edit().putString("available_squads", squadsJson).apply();
    }

    private void showUsersDialog() {
        List<User> users = loadUsers();
        if (users.isEmpty()) {
            Toast.makeText(requireContext(), "Нет зарегистрированных пользователей", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogUsersBinding dialogBinding = DialogUsersBinding.inflate(LayoutInflater.from(requireContext()));
        UserAdapter adapter = new UserAdapter(requireContext(), users, null, this::deleteUser);

        dialogBinding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        dialogBinding.recyclerViewUsers.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Список пользователей")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void deleteUser(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить пользователя")
                .setMessage("Вы уверены, что хотите удалить пользователя " + user.getFirstName() + " " + user.getLastName() + "?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("user_" + user.getLogin() + "_password");
                    editor.remove("user_" + user.getLogin() + "_first_name");
                    editor.remove("user_" + user.getLogin() + "_last_name");
                    editor.remove("user_" + user.getLogin() + "_role");
                    editor.remove("user_" + user.getLogin() + "_squads");
                    editor.remove("user_" + user.getLogin() + "_commander_squad");
                    editor.remove("user_" + user.getLogin() + "_curator_direction");
                    editor.apply();
                    Toast.makeText(requireContext(), "Пользователь " + user.getFirstName() + " удален", Toast.LENGTH_SHORT).show();
                    showUsersDialog();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private List<User> loadUsers() {
        List<User> userList = new ArrayList<>();
        Set<String> keys = sharedPreferences.getAll().keySet();
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();

        for (String key : keys) {
            if (key.startsWith("user_") && key.endsWith("_password")) {
                String login = key.replace("user_", "").replace("_password", "");
                String firstName = sharedPreferences.getString("user_" + login + "_first_name", "");
                String lastName = sharedPreferences.getString("user_" + login + "_last_name", "");
                String role = sharedPreferences.getString("user_" + login + "_role", "fighter");
                String squadsJson = sharedPreferences.getString("user_" + login + "_squads", "");
                List<Squad> squads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
                userList.add(new User(login, firstName, lastName, role, squads));
            }
        }
        return userList;
    }

    private void showAssignCommanderDialog() {
        List<User> eligibleUsers = loadUsers().stream()
                .filter(user -> !user.getSquads().isEmpty())
                .collect(Collectors.toList());

        if (eligibleUsers.isEmpty()) {
            Toast.makeText(requireContext(), "Нет пользователей, состоящих в отрядах", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userNames = eligibleUsers.stream()
                .map(user -> user.getFirstName() + " " + user.getLastName() + " (" + user.getLogin() + ")")
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите пользователя")
                .setItems(userNames, (dialog, which) -> {
                    User selectedUser = eligibleUsers.get(which);
                    List<Squad> userSquads = selectedUser.getSquads();
                    if (userSquads.size() == 1) {
                        assignCommander(selectedUser, userSquads.get(0));
                    } else {
                        showSquadSelectionDialog(selectedUser);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showSquadSelectionDialog(User user) {
        String[] squadNames = user.getSquads().stream()
                .map(squad -> squad.getName() + " (" + squad.getDirection() + ")")
                .toArray(String[]::new);
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите отряд для " + user.getFirstName() + " " + user.getLastName())
                .setItems(squadNames, (dialog, which) -> {
                    Squad selectedSquad = user.getSquads().get(which);
                    assignCommander(user, selectedSquad);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void assignCommander(User user, Squad squad) {
        String currentCommanderSquads = sharedPreferences.getString("user_" + user.getLogin() + "_commander_squad", "");
        String updatedCommanderSquads = currentCommanderSquads.isEmpty() ? squad.getName() : currentCommanderSquads + "," + squad.getName();
        sharedPreferences.edit()
                .putString("user_" + user.getLogin() + "_role", "commander")
                .putString("user_" + user.getLogin() + "_commander_squad", updatedCommanderSquads)
                .apply();
        Toast.makeText(requireContext(), user.getFirstName() + " назначен командиром отряда " + squad.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showAssignCuratorDialog() {
        List<User> users = loadUsers();
        String[] userNames = users.stream()
                .map(user -> user.getFirstName() + " " + user.getLastName() + " (" + user.getLogin() + ")")
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите пользователя")
                .setItems(userNames, (dialog, which) -> {
                    User selectedUser = users.get(which);
                    showDirectionSelectionDialog(selectedUser);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDirectionSelectionDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите направление для " + user.getFirstName() + " " + user.getLastName())
                .setItems(directions.toArray(new String[0]), (dialog, which) -> {
                    String selectedDirection = directions.get(which);
                    assignCurator(user, selectedDirection);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void assignCurator(User user, String direction) {
        List<Squad> directionSquads = availableSquads.stream()
                .filter(squad -> squad.getDirection().equals(direction))
                .collect(Collectors.toList());

        List<Squad> userSquads = new ArrayList<>(user.getSquads());
        for (Squad squad : directionSquads) {
            if (!userSquads.contains(squad)) {
                userSquads.add(squad);
            }
        }

        String squadsJson = gson.toJson(userSquads);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_" + user.getLogin() + "_squads", squadsJson);
        editor.putString("user_" + user.getLogin() + "_role", "curator");
        editor.putString("user_" + user.getLogin() + "_curator_direction", direction);

        StringBuilder commanderSquads = new StringBuilder();
        for (Squad squad : directionSquads) {
            if (commanderSquads.length() > 0) commanderSquads.append(",");
            commanderSquads.append(squad.getName());
        }
        editor.putString("user_" + user.getLogin() + "_commander_squad", commanderSquads.toString());

        editor.apply();
        Toast.makeText(requireContext(), user.getFirstName() + " назначен куратором направления " + direction + " и командиром отрядов", Toast.LENGTH_SHORT).show();
    }

    private void showAddSquadDialog() {
        DialogSquadBinding dialogBinding = DialogSquadBinding.inflate(LayoutInflater.from(requireContext()));
        new AlertDialog.Builder(requireContext())
                .setTitle("Добавить отряд")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = dialogBinding.editTextSquadName.getText().toString().trim();
                    String direction = dialogBinding.editTextSquadDirection.getText().toString().trim();
                    String description = dialogBinding.editTextSquadDescription.getText().toString().trim();

                    if (name.isEmpty() || direction.isEmpty()) {
                        Toast.makeText(requireContext(), "Название и направление обязательны", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Squad newSquad = new Squad(name, direction, description);
                    if (availableSquads.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
                        Toast.makeText(requireContext(), "Отряд с таким названием уже существует", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    availableSquads.add(newSquad);
                    saveAvailableSquads();
                    if (!directions.contains(direction)) {
                        directions.add(direction);
                    }
                    Toast.makeText(requireContext(), "Отряд " + name + " добавлен", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditSquadDialog() {
        if (availableSquads.isEmpty()) {
            Toast.makeText(requireContext(), "Нет отрядов для редактирования", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] squadNames = availableSquads.stream()
                .map(squad -> squad.getName() + " (" + squad.getDirection() + ")")
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите отряд для редактирования")
                .setItems(squadNames, (dialog, which) -> {
                    Squad selectedSquad = availableSquads.get(which);
                    showEditSquadDetailsDialog(selectedSquad);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditSquadDetailsDialog(Squad squad) {
        DialogSquadBinding dialogBinding = DialogSquadBinding.inflate(LayoutInflater.from(requireContext()));
        dialogBinding.editTextSquadName.setText(squad.getName());
        dialogBinding.editTextSquadDirection.setText(squad.getDirection());
        dialogBinding.editTextSquadDescription.setText(squad.getDescription());

        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать отряд")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newName = dialogBinding.editTextSquadName.getText().toString().trim();
                    String newDirection = dialogBinding.editTextSquadDirection.getText().toString().trim();
                    String newDescription = dialogBinding.editTextSquadDescription.getText().toString().trim();

                    if (newName.isEmpty() || newDirection.isEmpty()) {
                        Toast.makeText(requireContext(), "Название и направление обязательны", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newName.equalsIgnoreCase(squad.getName()) &&
                            availableSquads.stream().anyMatch(s -> s.getName().equalsIgnoreCase(newName))) {
                        Toast.makeText(requireContext(), "Отряд с таким названием уже существует", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String oldName = squad.getName();
                    squad.setName(newName);
                    squad.setDirection(newDirection);
                    squad.setDescription(newDescription);
                    updateUsersSquads(oldName, squad);
                    saveAvailableSquads();
                    updateDirections();
                    Toast.makeText(requireContext(), "Отряд " + newName + " обновлен", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDeleteSquadDialog() {
        if (availableSquads.isEmpty()) {
            Toast.makeText(requireContext(), "Нет отрядов для удаления", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] squadNames = availableSquads.stream()
                .map(squad -> squad.getName() + " (" + squad.getDirection() + ")")
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите отряд для удаления")
                .setItems(squadNames, (dialog, which) -> {
                    Squad selectedSquad = availableSquads.get(which);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Удалить отряд")
                            .setMessage("Вы уверены, что хотите удалить отряд " + selectedSquad.getName() + "?")
                            .setPositiveButton("Удалить", (dlg, w) -> {
                                removeSquadFromUsers(selectedSquad);
                                availableSquads.remove(selectedSquad);
                                saveAvailableSquads();
                                updateDirections();
                                Toast.makeText(requireContext(), "Отряд " + selectedSquad.getName() + " удален", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateUsersSquads(String oldName, Squad updatedSquad) {
        List<User> users = loadUsers();
        for (User user : users) {
            List<Squad> userSquads = new ArrayList<>(user.getSquads());
            for (int i = 0; i < userSquads.size(); i++) {
                if (userSquads.get(i).getName().equals(oldName)) {
                    userSquads.set(i, new Squad(updatedSquad.getName(), updatedSquad.getDirection(), updatedSquad.getDescription()));
                }
            }
            String squadsJson = gson.toJson(userSquads);
            String commanderSquads = sharedPreferences.getString("user_" + user.getLogin() + "_commander_squad", "");
            if (commanderSquads.contains(oldName)) {
                String updatedCommanderSquads = commanderSquads.replace(oldName, updatedSquad.getName());
                sharedPreferences.edit().putString("user_" + user.getLogin() + "_commander_squad", updatedCommanderSquads).apply();
            }
            sharedPreferences.edit().putString("user_" + user.getLogin() + "_squads", squadsJson).apply();
        }
    }

    private void removeSquadFromUsers(Squad squad) {
        List<User> users = loadUsers();
        for (User user : users) {
            List<Squad> userSquads = new ArrayList<>(user.getSquads());
            userSquads.removeIf(s -> s.getName().equals(squad.getName()));
            String squadsJson = gson.toJson(userSquads);
            String commanderSquads = sharedPreferences.getString("user_" + user.getLogin() + "_commander_squad", "");
            if (!commanderSquads.isEmpty()) {
                String updatedCommanderSquads = Arrays.stream(commanderSquads.split(","))
                        .filter(s -> !s.equals(squad.getName()))
                        .collect(Collectors.joining(","));
                sharedPreferences.edit().putString("user_" + user.getLogin() + "_commander_squad", updatedCommanderSquads).apply();
            }
            sharedPreferences.edit().putString("user_" + user.getLogin() + "_squads", squadsJson).apply();
        }
    }

    private void updateDirections() {
        directions = availableSquads.stream()
                .map(Squad::getDirection)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}