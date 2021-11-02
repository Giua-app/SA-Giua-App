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


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giua.app.ui.activities.TransparentUpdateDialogActivity;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class AppUpdateManager {

    final Connection session = Jsoup.newSession().ignoreContentType(true);
    Integer[] updateVer = {0,0,0};
    String tagName;
    Integer[] currentVer = {0,0,0};
    LoggerManager loggerManager;
    Context context;
    final String semverRegex = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";


    public AppUpdateManager(Context context){
        this.context = context;
        loggerManager = new LoggerManager("AppUpdateManager", this.context);
    }

    public void deleteOldApk(){
        String downloadLocation = context.getExternalFilesDir(null) + "/giua_update.apk";
        File file = new File(downloadLocation);

        if(!file.delete()){
            loggerManager.w("Errore nel cancellare file apk dell'aggiornamento! Forse si è aggiornato manualmente?");
        }
    }

    public boolean checkForUpdates(){
        loggerManager.d("Controllo aggiornamenti...");

        JsonNode rootNode = getReleasesJson();

        if (rootNode == null)    //Si è verificato un errore di qualche tipo
            return false;

        tagName = rootNode.findPath("tag_name").asText();

        loggerManager.d("Versione tag github: " + tagName);
        loggerManager.d("Versione app: " + BuildConfig.VERSION_NAME);
        String[] temp = BuildConfig.VERSION_NAME.split("-")[0].split("\\.");
        currentVer[0] = Integer.parseInt(temp[0]);
        currentVer[1] = Integer.parseInt(temp[1]);
        currentVer[2] = Integer.parseInt(temp[2]);
        if (tagName.matches(semverRegex)) {
            temp = tagName.split("-")[0].split("\\.");
            updateVer[0] = Integer.parseInt(temp[0]);
            updateVer[1] = Integer.parseInt(temp[1]);
            updateVer[2] = Integer.parseInt(temp[2]);
        } else {
            //Non è una versione, esci silenziosamente
            loggerManager.w("Versione tag trovata su github non rispetta SemVer, annullo");
            return false;
        }

        if(isUpdateNewerThanApp()){
            loggerManager.w("Rilevata nuova versione");
            return true;
        }
        loggerManager.w("Nessuna nuova versione rilevata");
        return false;
    }

    public JsonNode getReleasesJson(){
        String response = "";
        try {
            response = session.newRequest()
                    .url("https://api.github.com/repos/giua-app/giua-app/releases/latest")
                    .get().text();
        } catch (IOException e) {
            loggerManager.e("Impossibile contattare API di github! - " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(response);
        } catch (IOException e) {
            loggerManager.e("Impossibile leggere json! - " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return rootNode;
    }

    private boolean isUpdateNewerThanApp(){
        if (currentVer[0].equals(updateVer[0]) && currentVer[1].equals(updateVer[1]) && currentVer[2].equals(updateVer[2])) {
            //Nessun aggiornamento
            return false;
        }

        //false = versione vecchia, true = versione nuova
        return currentVer[0] <= updateVer[0] && currentVer[1] <= updateVer[1] && currentVer[2] <= updateVer[2];
    }

    /**
     * Controlla LastUpdateReminderDate
     * @return true se si può inviare l'update, false se bisogna aspettare ad un altro giorno
     */
    public boolean checkUpdateReminderDate(){
        int dayOfYear;
        int year;
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)-1);
        try {
            dayOfYear = Integer.parseInt(AppData.getLastUpdateReminderDate(context).split("#")[0]);
            year = Integer.parseInt(AppData.getLastUpdateReminderDate(context).split("#")[1]);
        } catch(Exception e){
            loggerManager.e("Errore critico nel parsing di LastUpdateReminder, è possibile che non sia mai stato notificato?");
            loggerManager.e("Sovrascrivo LastReminder con la data di ieri e notifico l'update");
            AppData.saveLastUpdateReminderDate(context, yesterdayCal);
            return true;
        }
        loggerManager.d("L'ultima volta che ho ricordato l'aggiornamento è stato il " + dayOfYear + "° giorno dell'anno " + year);
        loggerManager.d("Oggi è " + Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + ", ieri invece era il " + yesterdayCal.get(Calendar.DAY_OF_YEAR));


        if(Calendar.getInstance().get(Calendar.YEAR) != year){
            loggerManager.e("Errore, anno diverso da quello corrente. " +
                    "Non è possibile confrontare ReminderDate, cambio reminder a ieri e avviso l'utente dell'update");
            AppData.saveLastUpdateReminderDate(context, yesterdayCal);
            return true;
        }

        if(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) > dayOfYear){
            loggerManager.d("Reminder passato, bisogna ricordare l'utente dell'update");
            return true;
        }

        loggerManager.w("Il LastReminder è di oggi (o indietro nel tempo). Update gia notificato non c'è bisogno di rifarlo di nuovo");
        return false;
    }


    public void createNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        loggerManager.d("Creo notifica aggiornamento...");

        String title = "Nuova versione rilevata (" + tagName + ")";
        String description = "Clicca per informazioni";

        Intent intent = new Intent(context, TransparentUpdateDialogActivity.class);
        intent.putExtra("json", getReleasesJson().toString());
        PendingIntent pendingIntent;

        pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1")
                .setSmallIcon(R.drawable.ic_giuaschool_black)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(15, builder.build());
    }

    public void startUpdateDialog(){
        Intent intent = new Intent(context, TransparentUpdateDialogActivity.class);
        intent.putExtra("json", getReleasesJson().toString());
        context.startActivity(intent);
    }

}
