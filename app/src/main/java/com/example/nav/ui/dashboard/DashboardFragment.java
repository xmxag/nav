package com.example.nav.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.nav.Booking;
import com.example.nav.Squad;
import com.example.nav.databinding.FragmentDashboardBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private String loggedInUser;
    private List<Squad> userSquads;
    private List<Booking> bookings;
    private Map<String, Integer> squadColorMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        gson = new Gson();
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        loadUserSquads();
        loadBookings();
        initSquadColors();

        setupCalendar();

        return binding.getRoot();
    }

    private void loadUserSquads() {
        String squadsJson = sharedPreferences.getString("user_" + loggedInUser + "_squads", "");
        Type squadListType = new TypeToken<List<Squad>>() {}.getType();
        userSquads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
    }

    private void loadBookings() {
        String bookingsJson = sharedPreferences.getString("bookings", "");
        Type bookingListType = new TypeToken<List<Booking>>() {}.getType();
        bookings = bookingsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(bookingsJson, bookingListType);
    }

    private void initSquadColors() {
        squadColorMap = new HashMap<>();
        int[] colors = {
                com.example.nav.R.color.red,
                com.example.nav.R.color.blue,
                com.example.nav.R.color.green,
                com.example.nav.R.color.yellow,
                com.example.nav.R.color.purple,
                com.example.nav.R.color.orange,
                com.example.nav.R.color.cyan
        };
        List<Squad> allSquads = loadAllSquads();
        for (int i = 0; i < allSquads.size(); i++) {
            squadColorMap.put(allSquads.get(i).getName(), colors[i % colors.length]);
        }
    }

    private List<Squad> loadAllSquads() {
        String squadsJson = sharedPreferences.getString("available_squads", "");
        Type squadListType = new TypeToken<List<Squad>>() {}.getType();
        return squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
    }

    private void setupCalendar() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Map<Long, List<Booking>> bookingsByDate = new HashMap<>();

        for (Booking booking : bookings) {
            if (userSquads.stream().anyMatch(s -> s.getName().equals(booking.getSquadName()))) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dateFormat.parse(booking.getDate()));
                    long dateMillis = cal.getTimeInMillis();
                    bookingsByDate.computeIfAbsent(dateMillis, k -> new ArrayList<>()).add(booking);
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        }

        binding.calendarView2.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            String dateStr = dateFormat.format(selectedDate.getTime());
            List<Booking> bookingsOnDate = bookings.stream()
                    .filter(b -> b.getDate().equals(dateStr))
                    .filter(b -> userSquads.stream().anyMatch(s -> s.getName().equals(b.getSquadName())))
                    .collect(Collectors.toList());

            if (!bookingsOnDate.isEmpty()) {
                StringBuilder message = new StringBuilder("Бронирования на " + dateStr + ":\n");
                for (Booking booking : bookingsOnDate) {
                    message.append("• ").append(booking.getPurpose())
                            .append(" (").append(booking.getStartTime()).append(")\n");
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Бронирования")
                        .setMessage(message.toString())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}