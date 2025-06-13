package com.example.nav.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.nav.Booking;
import com.example.nav.Event;
import com.example.nav.Squad;
import com.example.nav.databinding.FragmentNotificationsBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private String loggedInUser;
    private List<Squad> userSquads;
    private List<Event> events;
    private List<Booking> bookings;
    private long lastUpdateTime;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        gson = new Gson();
        loggedInUser = sharedPreferences.getString("logged_in_user", "");
        loadUserSquads();
        loadEvents();
        loadBookings();

        // Check for updates
        long currentUpdateTime = sharedPreferences.getLong("last_update_time", 0);
        if (currentUpdateTime > lastUpdateTime) {
            Toast.makeText(requireContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
            lastUpdateTime = currentUpdateTime;
        }

        // Setup RecyclerView
        List<NotificationItem> notificationItems = getNotificationItems();
        NotificationAdapter adapter = new NotificationAdapter(requireContext(), notificationItems);
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNotifications.setAdapter(adapter);

        return binding.getRoot();
    }

    private void loadUserSquads() {
        String squadsJson = sharedPreferences.getString("user_" + loggedInUser + "_squads", "");
        Type squadListType = new TypeToken<List<Squad>>(){}.getType();
        userSquads = squadsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(squadsJson, squadListType);
    }

    private void loadEvents() {
        String eventsJson = sharedPreferences.getString("events", "");
        Type eventListType = new TypeToken<List<Event>>(){}.getType();
        events = eventsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(eventsJson, eventListType);
    }

    private void loadBookings() {
        String bookingsJson = sharedPreferences.getString("bookings", "");
        Type bookingListType = new TypeToken<List<Booking>>(){}.getType();
        bookings = bookingsJson.isEmpty() ? new ArrayList<>() : gson.fromJson(bookingsJson, bookingListType);
    }

    private List<NotificationItem> getNotificationItems() {
        List<NotificationItem> items = new ArrayList<>();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        // Add events for user's squads
        for (Event event : events) {
            if (userSquads.stream().anyMatch(s -> s.getName().equals(event.getSquadName()))) {
                String title = "Мероприятие: " + event.getName();
                String details = "Дата: " + event.getDate() + "\n" +
                        "Время: " + event.getStartTime() + " - " + event.getEndTime() + "\n" +
                        "Место: " + event.getLocation() + "\n" +
                        "Ответственный: " + event.getResponsible();
                try {
                    long timestamp = dateTimeFormat.parse(event.getDate() + " " + event.getStartTime()).getTime();
                    items.add(new NotificationItem(title, details, timestamp));
                } catch (ParseException e) {
                    items.add(new NotificationItem(title, details, 0));
                }
            }
        }

        // Add bookings for user's squads with notification enabled
        for (Booking booking : bookings) {
            if (booking.isNotifyFighters() && userSquads.stream().anyMatch(s -> s.getName().equals(booking.getSquadName()))) {
                String title = "Бронирование штаба";
                String details = "Дата: " + booking.getDate() + "\n" +
                        "Время: " + booking.getStartTime() + " - " + booking.getEndTime() + "\n" +
                        "Цель: " + booking.getPurpose() + "\n" +
                        "Ответственный: " + booking.getResponsible();
                try {
                    long timestamp = dateTimeFormat.parse(booking.getDate() + " " + booking.getStartTime()).getTime();
                    items.add(new NotificationItem(title, details, timestamp));
                } catch (ParseException e) {
                    items.add(new NotificationItem(title, details, 0));
                }
            }
        }

        // Sort by timestamp (newest first)
        items.sort(Comparator.comparingLong(NotificationItem::getTimestamp).reversed());
        return items;
    }

    private static class NotificationItem {
        private final String title;
        private final String details;
        private final long timestamp;

        NotificationItem(String title, String details, long timestamp) {
            this.title = title;
            this.details = details;
            this.timestamp = timestamp;
        }

        String getTitle() {
            return title;
        }

        String getDetails() {
            return details;
        }

        long getTimestamp() {
            return timestamp;
        }
    }

    private static class NotificationAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final Context context;
        private final List<NotificationItem> items;

        NotificationAdapter(Context context, List<NotificationItem> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(com.example.nav.R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            holder.textViewTitle.setText(item.getTitle());
            holder.textViewDetails.setText(item.getDetails());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final TextView textViewTitle;
            final TextView textViewDetails;

            ViewHolder(View itemView) {
                super(itemView);
                textViewTitle = itemView.findViewById(com.example.nav.R.id.textViewTitle);
                textViewDetails = itemView.findViewById(com.example.nav.R.id.textViewDetails);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}