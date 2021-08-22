/*
 * Giua App
 * Android app to view data from the giua@school workbook
 * Copyright (C) 2021 - 2021 Hiem, Franck1421 and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package com.giua.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.giua.webscraper.GiuaScraper;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;

    TextView tvUsername;
    TextView tvUserType;
    DrawerLayout drawerLayout;
    NavigationView navigationView;     //Il navigation drawer vero e proprio
    NavController navController;     //Si puo intendere come il manager dei fragments
    Button btnLogout;
    Button btnSettings;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnLogout = findViewById(R.id.nav_drawer_logout_button);
        btnSettings = findViewById(R.id.nav_drawer_settings_button);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_voti, R.id.nav_agenda, R.id.nav_lezioni, R.id.nav_bacheca, R.id.nav_pagella)
                .setOpenableLayout(drawerLayout)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);


        navigationView.setNavigationItemSelectedListener(this);

        tvUsername = navigationView.getHeaderView(0).findViewById(R.id.txtTitle);
        tvUserType = navigationView.getHeaderView(0).findViewById(R.id.txtUserType);

        navigationView.setCheckedItem(R.id.nav_voti);

        btnLogout.setOnClickListener(this::logoutButtonClick);
        btnSettings.setOnClickListener(this::settingsButtonClick);

        new Thread(() -> {
            GiuaScraper.userTypes userType = GlobalVariables.gS.getUserType();
            String user = GlobalVariables.gS.loadUserFromDocument();
            this.runOnUiThread(() -> {
                if (userType == GiuaScraper.userTypes.PARENT)
                    tvUserType.setText("Genitore");
                else if (userType == GiuaScraper.userTypes.STUDENT)
                    tvUserType.setText("Studente");
                tvUsername.setText(user);
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        if (item.isChecked()) {
            closeNavDrawer();
        } else if (item.getItemId() == R.id.nav_voti) {
            startVotesFragment();
        } else if (item.getItemId() == R.id.nav_agenda) {
            startAgendaFragment();
        } else if (item.getItemId() == R.id.nav_lezioni) {
            startLessonsFragment();
        } else if (item.getItemId() == R.id.nav_bacheca) {
            startNewsLetterFragment();
        } else if (item.getItemId() == R.id.nav_pagella) {
            startReportCardFragment();
        }

        return true;
    }

    private void closeNavDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void logoutButtonClick(View view) {
        Intent intent = new Intent(this, ActivityManager.class);
        LoginData.clearAll(this);
        startActivity(intent);
        finish();
    }

    private void settingsButtonClick(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void startReportCardFragment() {
        handler.post(() -> navController.navigate(R.id.nav_pagella, null));
        closeNavDrawer();
    }

    private void startNewsLetterFragment() {
        handler.post(() -> navController.navigate(R.id.nav_bacheca, null));
        closeNavDrawer();
    }

    private void startVotesFragment() {
        handler.post(() -> navController.navigate(R.id.nav_voti, null));
        closeNavDrawer();
    }

    private void startLessonsFragment() {
        handler.post(() -> navController.navigate(R.id.nav_lezioni, null));
        closeNavDrawer();
    }

    private void startAgendaFragment() {
        handler.post(() -> navController.navigate(R.id.nav_agenda, null));
        closeNavDrawer();
    }

    public static void setErrorMessage(String message, View root, int layoutID, NavController _navController) {
        if (Objects.requireNonNull(_navController.getCurrentDestination()).getId() == layoutID)
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }


    @Override
    public void onBackPressed() {
        if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.nav_voti) {
            navigationView.setCheckedItem(R.id.nav_voti);
            startVotesFragment();
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onRestart() {
        onRestoreInstanceState(new Bundle());
        releaseInstance();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        onRestoreInstanceState(new Bundle());
        releaseInstance();
        super.onResume();
    }

    @Override
    protected void onPause() {
        onSaveInstanceState(new Bundle());
        super.onPause();
    }

    @Override
    protected void onStop() {
        onSaveInstanceState(new Bundle());
        super.onStop();
    }
}