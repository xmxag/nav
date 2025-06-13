package com.example.nav.ui.comsos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nav.Booking;
import com.example.nav.Event;
import com.example.nav.Squad;
import com.example.nav.User;
import com.example.nav.UserAdapter;
import com.example.nav.databinding.DialogBookingBinding;
import com.example.nav.databinding.DialogEditBookingBinding;
import com.example.nav.databinding.DialogEditEventBinding;
import com.example.nav.databinding.DialogEventBinding;
import com.example.nav.databinding.DialogSquadMembersBinding;
import com.example.nav.databinding.FragmentComsosBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import com.example.nav.R;

public class ComsosFragment extends Fragment {

    private FragmentComsosBinding binding;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private String loggedInUser;
    private String userRole;
    private List<String> commanderSquads;
    private List<Squad> availableSquads;
    private List<Booking> bookings;
    private List<Event> events;
    private List<User> users;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentComsosBinding.inflate(inflater, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        gson = new Gson();
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        userRole = sharedPreferences.getString("user_" + loggedInUser + "_role", "fighter");
        commanderSquads = loadUserSquads();
        loadSquads();
        loadBookings();
        loadEvents();
        loadUsers();

        binding.buttonPlanEvent.setOnClickListener(v -> handlePlanEvent());
        binding.buttonBookHeadquarters.setOnClickListener(v -> handleBookHeadquarters());
        binding.buttonManageTeam.setOnClickListener(v -> handleManageTeam());
        binding.buttonBookings.setOnClickListener(v -> showMyBookings());
        binding.buttonEvents.setOnClickListener(v -> showMyEvents());

        return binding.getRoot();
    }

    private List<String> loadUserSquads() {
        String squads = sharedPreferences.getString("user_" + loggedInUser + "_commander_squad", "");
        return squads.isEmpty() ? new ArrayList<>() : Arrays.asList(squads.split(","));
    }

    private void loadSquads() {
        String squadsJson = sharedPreferences.getString("available_squads", "");
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        availableSquads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
    }

    private void loadBookings() {
        String bookingsJson = sharedPreferences.getString("bookings", "");
        Type bookingListType = new TypeToken<List<Booking>>(){}.getType();
        bookings = bookingsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(bookingsJson, bookingListType);
    }

    private void loadEvents() {
        String eventsJson = sharedPreferences.getString("events", "");
        Type eventListType = new TypeToken<List<Event>>(){}.getType();
        events = eventsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(eventsJson, eventListType);
    }

    private void loadUsers() {
        String usersJson = sharedPreferences.getString("users", "");
        Type userListType = new TypeToken<List<User>>(){}.getType();
        users = usersJson.isEmpty() ? new ArrayList<>() : gson.fromJson(usersJson, userListType);
    }

    private void saveBookings() {
        String bookingsJson = gson.toJson(bookings);
        sharedPreferences.edit()
                .putString("bookings", bookingsJson)
                .putLong("last_update_time", System.currentTimeMillis())
                .apply();
    }

    private void saveEvents() {
        String eventsJson = gson.toJson(events);
        sharedPreferences.edit()
                .putString("events", eventsJson)
                .putLong("last_update_time", System.currentTimeMillis())
                .apply();
    }

    private void handlePlanEvent() {
        if (commanderSquads.isEmpty()) {
            Toast.makeText(requireContext(), "У вас нет отрядов для планирования мероприятий", Toast.LENGTH_SHORT).show();
            return;
        }
        if (commanderSquads.size() == 1) {
            showEventDialog(commanderSquads.get(0));
        } else {
            showSquadSelectionDialog(commanderSquads, this::showEventDialog);
        }
    }

    private void handleBookHeadquarters() {
        if (!userRole.equals("admin") && commanderSquads.isEmpty()) {
            Toast.makeText(requireContext(), "Только администраторы или командиры могут бронировать штаб", Toast.LENGTH_SHORT).show();
            return;
        }
        if (commanderSquads.size() == 1 || userRole.equals("admin")) {
            showBookingDialog(commanderSquads.isEmpty() ? null : commanderSquads.get(0));
        } else {
            showSquadSelectionDialog(commanderSquads, this::showBookingDialog);
        }
    }

    private void handleManageTeam() {
        if (commanderSquads.isEmpty()) {
            Toast.makeText(requireContext(), "У вас нет отрядов для управления", Toast.LENGTH_SHORT).show();
            return;
        }
        if (commanderSquads.size() == 1) {
            showSquadMembersDialog(commanderSquads.get(0));
        } else {
            showSquadSelectionDialog(commanderSquads, this::showSquadMembersDialog);
        }
    }

    private void showSquadSelectionDialog(List<String> squads, SquadSelectionCallback callback) {
        String[] squadNames = squads.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите отряд")
                .setItems(squadNames, (dialog, which) -> callback.onSelectSquad(squadNames[which]))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEventDialog(String squadName) {
        DialogEventBinding dialogBinding = DialogEventBinding.inflate(LayoutInflater.from(requireContext()));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        dialogBinding.textViewEventDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.textViewEventDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.textViewEventStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewEventStartTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        dialogBinding.textViewEventEndTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewEventEndTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Планировать мероприятие")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = dialogBinding.editTextEventName.getText().toString().trim();
                    String location = dialogBinding.editTextEventLocation.getText().toString().trim();
                    String date = dialogBinding.textViewEventDate.getText().toString().trim();
                    String startTime = dialogBinding.textViewEventStartTime.getText().toString().trim();
                    String endTime = dialogBinding.textViewEventEndTime.getText().toString().trim();

                    if (name.isEmpty() || location.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                        Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isValidTimeRange(startTime, endTime)) {
                        Toast.makeText(requireContext(), "Время конца не может быть раньше времени начала", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Event event = new Event(name, location, date, startTime, endTime, squadName, loggedInUser);
                    events.add(0, event);
                    saveEvents();
                    Toast.makeText(requireContext(), "Мероприятие запланировано", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showBookingDialog(String squadName) {
        DialogBookingBinding dialogBinding = DialogBookingBinding.inflate(LayoutInflater.from(requireContext()));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (!userRole.equals("admin") && squadName != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new String[]{squadName});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerSquad.setAdapter(adapter);
            dialogBinding.spinnerSquad.setEnabled(false);
        } else if (userRole.equals("admin")) {
            List<String> squadNames = availableSquads.stream().map(Squad::getName).collect(Collectors.toList());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, squadNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogBinding.spinnerSquad.setAdapter(adapter);
        }

        dialogBinding.textViewBookingDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.textViewBookingDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.textViewBookingStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewBookingStartTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        dialogBinding.textViewBookingEndTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewBookingEndTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Бронировать штаб")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String selectedSquad = dialogBinding.spinnerSquad.getSelectedItem() != null ?
                            dialogBinding.spinnerSquad.getSelectedItem().toString() : squadName;
                    String date = dialogBinding.textViewBookingDate.getText().toString().trim();
                    String startTime = dialogBinding.textViewBookingStartTime.getText().toString().trim();
                    String endTime = dialogBinding.textViewBookingEndTime.getText().toString().trim();
                    String purpose = dialogBinding.editTextBookingPurpose.getText().toString().trim();
                    boolean notifyFighters = dialogBinding.checkBoxNotifyFighters.isChecked();

                    if (selectedSquad == null || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || purpose.isEmpty()) {
                        Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!isValidTimeRange(startTime, endTime)) {
                        Toast.makeText(requireContext(), "Время конца не может быть раньше времени начала", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (isBookingConflict(date, startTime, endTime, null)) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Конфликт бронирования")
                                .setMessage("На указанную дату и время уже есть бронирование. Выберите другую дату или время.")
                                .setPositiveButton("ОК", null)
                                .show();
                        return;
                    }

                    Booking booking = new Booking(selectedSquad, date, startTime, endTime, purpose, loggedInUser, notifyFighters);
                    bookings.add(0, booking);
                    saveBookings();
                    Toast.makeText(requireContext(), "Штаб забронирован", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showMyBookings() {
        List<Booking> myBookings = bookings.stream()
                .filter(b -> b.getResponsible().equals(loggedInUser))
                .collect(Collectors.toList());
        showBookingsDialog(myBookings, "Мои бронирования");
    }

    private void showBookingsDialog(List<Booking> bookingsList, String title) {
        if (bookingsList.isEmpty()) {
            Toast.makeText(requireContext(), "Нет бронирований", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] bookingDescriptions = bookingsList.stream()
                .map(b -> "Отряд: " + b.getSquadName() + ", Дата: " + b.getDate() + ", Время: " + b.getStartTime() + " - " + b.getEndTime())
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(bookingDescriptions, (dialog, which) -> {
                    Booking selectedBooking = bookingsList.get(which);
                    showEditBookingDialog(selectedBooking);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditBookingDialog(Booking booking) {
        DialogEditBookingBinding dialogBinding = DialogEditBookingBinding.inflate(LayoutInflater.from(requireContext()));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        dialogBinding.textViewSquad.setText("Отряд: " + booking.getSquadName());
        dialogBinding.textViewBookingDate.setText(booking.getDate());
        dialogBinding.textViewBookingStartTime.setText(booking.getStartTime());
        dialogBinding.textViewBookingEndTime.setText(booking.getEndTime());
        dialogBinding.editTextBookingPurpose.setText(booking.getPurpose());
        dialogBinding.checkBoxNotifyFighters.setChecked(booking.isNotifyFighters());

        dialogBinding.textViewBookingDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.textViewBookingDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.textViewBookingStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewBookingStartTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        dialogBinding.textViewBookingEndTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewBookingEndTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать бронирование")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String date = dialogBinding.textViewBookingDate.getText().toString().trim();
                    String startTime = dialogBinding.textViewBookingStartTime.getText().toString().trim();
                    String endTime = dialogBinding.textViewBookingEndTime.getText().toString().trim();
                    String purpose = dialogBinding.editTextBookingPurpose.getText().toString().trim();
                    boolean notifyFighters = dialogBinding.checkBoxNotifyFighters.isChecked();

                    if (date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || purpose.isEmpty()) {
                        Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!isValidTimeRange(startTime, endTime)) {
                        Toast.makeText(requireContext(), "Время конца не может быть раньше времени начала", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (isBookingConflict(date, startTime, endTime, booking)) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Конфликт бронирования")
                                .setMessage("На указанную дату и время уже есть бронирование. Выберите другую дату или время.")
                                .setPositiveButton("ОК", null)
                                .show();
                        return;
                    }

                    bookings.remove(booking);
                    Booking updatedBooking = new Booking(booking.getSquadName(), date, startTime, endTime, purpose, booking.getResponsible(), notifyFighters);
                    bookings.add(0, updatedBooking);
                    saveBookings();
                    Toast.makeText(requireContext(), "Бронирование обновлено", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Удалить", (dialog, which) -> {
                    bookings.remove(booking);
                    saveBookings();
                    Toast.makeText(requireContext(), "Бронирование удалено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showMyEvents() {
        List<Event> myEvents = events.stream()
                .filter(e -> e.getResponsible().equals(loggedInUser))
                .collect(Collectors.toList());
        showEventsDialog(myEvents);
    }

    private void showEventsDialog(List<Event> eventsList) {
        if (eventsList.isEmpty()) {
            Toast.makeText(requireContext(), "Нет мероприятий", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] eventDescriptions = eventsList.stream()
                .map(e -> "Название: " + e.getName() + ", Дата: " + e.getDate() + ", Время: " + e.getStartTime() + " - " + e.getEndTime())
                .toArray(String[]::new);

        new AlertDialog.Builder(requireContext())
                .setTitle("Мои мероприятия")
                .setItems(eventDescriptions, (dialog, which) -> {
                    Event selectedEvent = eventsList.get(which);
                    showEditEventDialog(selectedEvent);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditEventDialog(Event event) {
        DialogEditEventBinding dialogBinding = DialogEditEventBinding.inflate(getLayoutInflater());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        dialogBinding.editTextEventName.setText(event.getName());
        dialogBinding.editTextEventLocation.setText(event.getLocation());
        dialogBinding.textViewEventSquad.setText("Отряд: " + event.getSquadName());
        dialogBinding.textViewEventDate.setText(event.getDate());
        dialogBinding.textViewEventStartTime.setText(event.getStartTime());
        dialogBinding.textViewEventEndTime.setText(event.getEndTime());

        dialogBinding.textViewEventDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dialogBinding.textViewEventDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.textViewEventStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewEventStartTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        dialogBinding.textViewEventEndTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dialogBinding.textViewEventEndTime.setText(timeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать мероприятие")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = dialogBinding.editTextEventName.getText().toString().trim();
                    String location = dialogBinding.editTextEventLocation.getText().toString().trim();
                    String date = dialogBinding.textViewEventDate.getText().toString().trim();
                    String startTime = dialogBinding.textViewEventStartTime.getText().toString().trim();
                    String endTime = dialogBinding.textViewEventEndTime.getText().toString().trim();

                    if (name.isEmpty() || location.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                        Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isValidTimeRange(startTime, endTime)) {
                        Toast.makeText(requireContext(), "Время окончания не может быть раньше времени начала", Toast.LENGTH_LONG).show();
                        return;
                    }

                    events.remove(event);
                    Event updatedEvent = new Event(name, location, date, startTime, endTime, event.getSquadName(), event.getResponsible());
                    events.add(0, updatedEvent);
                    saveEvents();
                    Toast.makeText(requireContext(), "Мероприятие обновлено", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Удалить", (dialog, which) -> {
                    events.remove(event);
                    saveEvents();
                    Toast.makeText(requireContext(), "Мероприятие удалено", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private boolean isValidTimeRange(String startTime, String endTime) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            long start = timeFormat.parse(startTime).getTime();
            long end = timeFormat.parse(endTime).getTime();
            return end > start;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBookingConflict(String date, String startTime, String endTime, Booking excludeBooking) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            long newStart = timeFormat.parse(startTime).getTime();
            long newEnd = timeFormat.parse(endTime).getTime();

            for (Booking booking : bookings) {
                if (booking != excludeBooking && booking.getDate().equals(date)) {
                    long existingStart = timeFormat.parse(booking.getStartTime()).getTime();
                    long existingEnd = timeFormat.parse(booking.getEndTime()).getTime();
                    if (!(newEnd <= existingStart || newStart >= existingEnd)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void showSquadMembersDialog(String squadName) {
        DialogSquadMembersBinding dialogBinding = DialogSquadMembersBinding.inflate(LayoutInflater.from(requireContext()));
        List<User> squadMembers = loadSquadMembers(squadName);
        UserAdapter adapter = new UserAdapter(requireContext(), squadMembers,
                user -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("user_login", user.getLogin());
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.action_navigation_comsos_to_profileFragment, bundle);
                },
                (user) -> removeUserFromSquad(user, squadName));

        dialogBinding.recyclerViewSquadMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        dialogBinding.recyclerViewSquadMembers.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Бойцы отряда: " + squadName)
                .setView(dialogBinding.getRoot())
                .setPositiveButton("ОК", null)
                .show();
    }

    private List<User> loadSquadMembers(String squadName) {
        List<User> squadMembers = new ArrayList<>();
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        Set<String> keys = sharedPreferences.getAll().keySet();

        for (String key : keys) {
            if (key.startsWith("user_") && key.endsWith("_password")) {
                String login = key.replace("user_", "").replace("_password", "");
                String squadsJson = sharedPreferences.getString("user_" + login + "_squads", "");
                if (!squadsJson.isEmpty()) {
                    List<Squad> squads = gson.fromJson(squadsJson, squadListType);
                    if (squads.stream().anyMatch(s -> s.getName().equals(squadName))) {
                        String firstName = sharedPreferences.getString("user_" + login + "_first_name", "");
                        String lastName = sharedPreferences.getString("user_" + login + "_last_name", "");
                        String role = sharedPreferences.getString("user_" + login + "_role", "fighter");
                        squadMembers.add(new User(login, firstName, lastName, role, squads));
                    }
                }
            }
        }
        return squadMembers;
    }

    private void removeUserFromSquad(User user, String squadName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить бойца")
                .setMessage("Удалить " + user.getFirstName() + " " + user.getLastName() + " из отряда " + squadName + "?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    List<Squad> userSquads = new ArrayList<>(user.getSquads());
                    userSquads.removeIf(s -> s.getName().equalsIgnoreCase(squadName));
                    String squadsJson = gson.toJson(userSquads);
                    sharedPreferences.edit().putString("user_" + user.getLogin() + "_squads", squadsJson).apply();
                    Toast.makeText(requireContext(), user.getFirstName() + " удалён из отряда", Toast.LENGTH_SHORT).show();
                    showSquadMembersDialog(squadName);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private interface SquadSelectionCallback {
        void onSelectSquad(String squadName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}