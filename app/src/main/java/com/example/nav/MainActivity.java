package com.example.nav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.nav.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
       // setSupportActionBar(findViewById(R.id.calendar_view));

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Get user role and commander squads
        String userRole = sharedPreferences.getString("user_role", "fighter");
        String loggedInUser = sharedPreferences.getString("logged_in_user", "");
        String commanderSquads = sharedPreferences.getString("user_" + loggedInUser + "_commander_squad", "");

        // Configure bottom navigation based on role
        BottomNavigationView navView = binding.navView;
        navView.getMenu().clear(); // Clear default menu

        // Add menu items based on role
        Menu menu = navView.getMenu();
        menu.add(Menu.NONE, R.id.navigation_home, Menu.NONE, R.string.title_home)
                .setIcon(R.drawable.ic_home_black_24dp);
        menu.add(Menu.NONE, R.id.navigation_notifications, Menu.NONE, R.string.title_notifications)
                .setIcon(R.drawable.ic_notifications_black_24dp);
        menu.add(Menu.NONE, R.id.navigation_dashboard, Menu.NONE, R.string.title_dashboard)
                .setIcon(R.drawable.ic_dashboard_black_24dp);

        if (userRole.equals("commander") || userRole.equals("admin") || (userRole.equals("curator") && !commanderSquads.isEmpty())) {
            menu.add(Menu.NONE, R.id.navigation_comsos, Menu.NONE, R.string.title_comsos)
                    .setIcon(R.drawable.ic_comsos);
        }

        if (userRole.equals("admin")) {
            menu.add(Menu.NONE, R.id.navigation_admin, Menu.NONE, R.string.title_admin)
                    .setIcon(R.drawable.ic_admin);
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_dashboard,
                R.id.navigation_comsos, R.id.navigation_admin)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        sharedPreferences.edit()
                                .remove("logged_in_user")
                                .remove("user_role")
                                .apply();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}