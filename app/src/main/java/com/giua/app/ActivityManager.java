/*
 * Giua App
 * Android app to view data from the giua@school workbook
 * Copyright (C) 2021 - 2022 Hiem, Franck1421 and contributors
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.giua.app.ui.activities.AccountsActivity.AccountsActivity;
import com.giua.app.ui.activities.AppIntroActivity;
import com.giua.app.ui.activities.AutomaticLoginActivity;
import com.giua.app.ui.activities.CaocActivity;
import com.giua.app.ui.activities.DrawerActivity;
import com.giua.app.ui.activities.LoginActivity;
import com.giua.webscraper.GiuaScraper;

import java.util.Calendar;

import cat.ereza.customactivityoncrash.config.CaocConfig;

/**
 * Questa è la prima activity ad essere avviata e serve a gestire quale activity
 * dovrà essere startata dopo.
 */
public class ActivityManager extends AppCompatActivity {

    LoggerManager loggerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loggerManager = new LoggerManager("ActivityManager", this);
        loggerManager.d("--@" + BuildConfig.VERSION_NAME + "      Build type: " + BuildConfig.BUILD_TYPE);
        loggerManager.d("onCreate chiamato");
        if (SettingsData.getSettingBoolean(this, SettingKey.DEMO_MODE))
            loggerManager.w("DEMO ATTIVATA");

        switch (SettingsData.getSettingString(this, SettingKey.THEME)) {
            case "0":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "1":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "2":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;

        }
        super.onCreate(savedInstanceState);

        if (GlobalVariables.gsThread == null)
            GlobalVariables.gsThread = new GiuaScraperThread();
        else if(GlobalVariables.gsThread.isInterrupting())
            GlobalVariables.gsThread.stopInterrupting();
        else if(GlobalVariables.gsThread.isInterrupted())
            GlobalVariables.gsThread.start();

        setupCaoc(); //Crash handler

        GiuaScraper.setDebugMode(true);

        if (!SettingsData.getSettingBoolean(this, SettingKey.NOT_FIRST_START))
            firstStart();

        final String defaultUrl = SettingsData.getSettingString(this, SettingKey.DEFAULT_URL);
        if (!defaultUrl.equals(""))
            GiuaScraper.setGlobalSiteUrl(defaultUrl);
        else
            SettingsData.saveSettingString(this, SettingKey.DEFAULT_URL, GiuaScraper.getGlobalSiteUrl());

        setupNotificationManager();

        executeDailyStartupActions();

        if (getIntent().getBooleanExtra("fromCAOC", false)) {
            loggerManager.d("Individuato crash precedente, invio segnalazione");
            new Analytics.Builder(Analytics.CRASH)
                    .addCustomValue("crash_stacktrace", getIntent().getStringExtra("stacktrace"))
                    .send();
        }

        checkForCompatibility();
        final int introStatus = AppData.getIntroStatus(this);

        /*
         * 2 Intro & welcomeBack vista
         * 1 Intro già vista
         * 0 Intro mai vista
         * -1 Intro mai vista (default se non impostato)
         * -2 App aggiornata (welcomeBack 061 e 062 mai visto)
         *
         */
        loggerManager.d("introStatus è " + introStatus);
        if (introStatus < 1) {
            if(introStatus != -2) //non è una prima installazione se è -2
                Analytics.sendDefaultRequest(Analytics.FIRST_START);

            loggerManager.d("Avvio App Intro Activity");
            Intent intent = new Intent(this, AppIntroActivity.class);
            intent.putExtra("welcomeBack", introStatus == -2);
            startActivity(intent);
            return;
        }

        checkForUpdates();
        checkForPreviousUpdate();

        Object[] allAccountUsernames = AppData.getAllAccountUsernames(this).toArray();

        if (AppData.getActiveUsername(this).equals("")) {
            if (allAccountUsernames.length > 1)
                startAccountsActivity();
            else if (allAccountUsernames.length == 1) {
                AppData.saveActiveUsername(this, allAccountUsernames[0].toString());
                startAutomaticLoginActivity();
            } else
                startLoginActivity();
        } else
            startAutomaticLoginActivity();
    }

    private void executeDailyStartupActions(){
        //Analytics.sendDefaultRequest("Avvio"); //Debug
        if(checkLastStartupDate()){
            //Analytics
            Analytics.sendDefaultRequest(Analytics.FIRST_DAILY_START);
            //Pulizia log
            loggerManager.cleanupLogs();

            AppData.saveLastStartupDate(this, Calendar.getInstance());
        }
    }

    /**
     * Controlla LastStartupDate (copiato da {@link AppUpdateManager#checkUpdateReminderDate()})
     * @return true se si può notificare l'avvio, false se bisogna aspettare ad un altro giorno
     */
    //TODO: trasferire in AppUtils
    public boolean checkLastStartupDate(){
        int dayOfYear;
        int year;
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)-1);
        try {
            dayOfYear = Integer.parseInt(AppData.getLastStartupDate(this).split("#")[0]);
            year = Integer.parseInt(AppData.getLastStartupDate(this).split("#")[1]);
        } catch(Exception e){
            loggerManager.e("Errore critico nel parsing di LastStartup, è possibile che non esista?");
            loggerManager.e("Sovrascrivo LastStartup con la data di ieri");
            AppData.saveLastUpdateReminderDate(this, yesterdayCal);
            return true;
        }
        loggerManager.d("L'ultimo avvio è stato il " + dayOfYear + "° giorno dell'anno " + year);
        loggerManager.d("Oggi è " + Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + ", ieri invece era il " + yesterdayCal.get(Calendar.DAY_OF_YEAR));


        if(Calendar.getInstance().get(Calendar.YEAR) != year){
            loggerManager.e("Errore, anno diverso da quello corrente. " +
                    "Non è possibile confrontare LastStartup, cambio data a ieri");
            AppData.saveLastUpdateReminderDate(this, yesterdayCal);
            return true;
        }

        if(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) > dayOfYear){
            loggerManager.d("Reminder passato, bisogna ricordare l'utente dell'update");
            return true;
        }

        loggerManager.w("Il LastStartup è di oggi (o avanti nel tempo).");
        return false;
    }



    private void checkForPreviousUpdate() {
        if (!AppData.getAppVersion(this).equals("")
                && !AppData.getAppVersion(this).equals(BuildConfig.VERSION_NAME)) {

            AppData.saveLastUpdateReminderDate(this, Calendar.getInstance()); //Imposta last reminder
        }

        //Non salviamo la stringa della versione per permettere a DrawerActivity e LoginActivity di fare le loro cose
        //SettingsData.saveSettingString(this, SettingKey.APP_VER, BuildConfig.VERSION_NAME);
    }

    /**
     * Controlla compatibilità tra la nuova versione e quella vecchia
     */
    private void checkForCompatibility() {
        final String lastVer = AppData.getAppVersion(this);
        final String appVer = BuildConfig.VERSION_NAME;

        //0.6.1
        if (!lastVer.contains(appVer) && lastVer.contains("0.6.1")
                && AppData.getIntroStatus(this) != 2) {

            CompatibilityManager.checkFor061Update(this);
        }

        //Serve perchè in nuove versioni la versione non è più su Settings
        final String oldVer = SettingsData.getSettingString(this, "appVersion");

        //0.6.2
        if (!oldVer.equals("") && oldVer.contains("0.6.2")) {
            CompatibilityManager.checkFor062Update(this);
        }

        //0.6.3
        if (!lastVer.contains(appVer) && lastVer.contains("0.6.3")
                && !AccountData.getSharedPreferencesForOldLogin(this).getString("user", "NOTFOUND").equals("NOTFOUND")) {
            CompatibilityManager.checkFor063Update(this);
        }
    }

    private void startDrawerActivity() {
        loggerManager.d("Avvio direttamente Drawer Activity dato che gS esiste già");
        String goTo = getIntent().getStringExtra("goTo");
        if (goTo == null)
            goTo = "";
        startActivity(new Intent(ActivityManager.this, DrawerActivity.class).putExtra("goTo", goTo));
        finish();
    }

    //Questa funzione viene chiamata solo al primo avvio di sempre dell'app
    private void firstStart() {
        SettingsData.saveSettingBoolean(this, SettingKey.NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.NOT_FIRST_START, true);
        SettingsData.saveSettingBoolean(this, SettingKey.NEWSLETTERS_NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.ALERTS_NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.UPDATES_NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.VOTES_NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.HOMEWORKS_NOTIFICATION, true);
        SettingsData.saveSettingBoolean(this, SettingKey.TESTS_NOTIFICATION, true);
    }

    private void startAccountsActivity() {
        loggerManager.d("Avvio AccountsActivity Activity");
        String goTo = getIntent().getStringExtra("goTo");
        if (goTo == null)
            goTo = "";
        startActivity(new Intent(ActivityManager.this, AccountsActivity.class)
                .putExtra("account_chooser_mode", true)
                .putExtra("goTo", goTo));
        finish();
    }

    private void startLoginActivity() {
        loggerManager.d("Avvio Main Login Activity");
        startActivity(new Intent(ActivityManager.this, LoginActivity.class));
        finish();
    }

    private void startAutomaticLoginActivity() {
        loggerManager.d("Avvio Automatic Login Activity");
        String goTo = getIntent().getStringExtra("goTo");
        if (goTo == null)
            goTo = "";
        startActivity(new Intent(ActivityManager.this, AutomaticLoginActivity.class).putExtra("goTo", goTo));
        finish();
    }

    private void checkForUpdates(){
        new Thread(() -> {
            AppUpdateManager manager = new AppUpdateManager(ActivityManager.this);
            if (SettingsData.getSettingBoolean(this, SettingKey.UPDATES_NOTIFICATION) && manager.checkForUpdates() && manager.checkUpdateReminderDate()) {
                manager.createNotification();
            }
        }).start();
    }

    private void setupCaoc() {
        //CAOC: CustomActivityOnCrash
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //crash silently when the app is in background
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .trackActivities(true)
                //This shows a different image on the error activity, instead of the default upside-down bug.
                //You may use a drawable or a mipmap.
                .errorDrawable(R.drawable.ic_giuaschool_logo1)
                .errorActivity(CaocActivity.class)
                .apply();
        loggerManager.d("CustomActivityOnCrash setup completato");
    }

    private void setupNotificationManager(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Giua App Novità";
            String description = "Notifiche dal registro elettronico";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("0", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            notificationManager.cancel(0);  //Cancella la notifica se cè

            CharSequence name2 = "Giua App Aggiornamenti";
            String description2 = "Aggiornamenti dell'app da github";
            int importance2 = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel2 = new NotificationChannel("1", name2, importance2);
            channel.setDescription(description2);
            NotificationManager notificationManager2 = getSystemService(NotificationManager.class);
            notificationManager2.createNotificationChannel(channel2);

        }
        loggerManager.d("Notification Manager setup completato");
    }


    /**
     * Esci dall'applicazione simulando la pressione del tasto home
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
