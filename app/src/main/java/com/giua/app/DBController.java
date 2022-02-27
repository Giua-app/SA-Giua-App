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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.giua.objects.Absence;
import com.giua.objects.Alert;
//agenda objects
import com.giua.objects.Activity;
import com.giua.objects.Homework;
import com.giua.objects.Test;

import com.giua.objects.Authorization;
import com.giua.objects.DisciplinaryNotices;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Controller per interagire con il database per la modalità offline
 */
public class DBController extends SQLiteOpenHelper {

    //!!!
    //FIXME: USARE SQLiteDatabase.releaseMemory() DOVE L'APP VA IN BACKGROUND O ALTRO

    private static final String DB_NAME = "giuapp_offline_data";
    private static final int DB_VERSION = 2;

    private static final String ALERTS_TABLE = "alerts";
    private static final String ABSENCES_TABLE ="absence";
    private static final String ACTIVITIES_TABLE ="activity";
    private static final String AUTHORIZATIONS_TABLE ="authorizations";
    private static final String DISCIPLINARY_NOTICES_TABLE="disciplinaryNotices";
    private static final String HOMEWORKS_TABLE="homeworks";
    private static final String TESTS_TABLE="tests";
    private static final String NEWSLETTERS_TABLE = "newsletters";

    /**
     * Crea un istanza DbController. Se il database non esiste, ne crea uno nuovo
     * @param context
     */
    public DBController(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {


        //region Crea tabella con nome alert con le colonne specificate
        String query = "CREATE TABLE " + ALERTS_TABLE + " ("
                + DBAlert.STATUS_COL + " TEXT, "
                + DBAlert.DATE_COL + " TEXT,"
                + DBAlert.RECEIVERS_COL + " TEXT,"
                + DBAlert.OBJECT_COL + " TEXT,"
                + DBAlert.PAGE_COL + " INTEGER,"
                + DBAlert.DETAILS_URL_COL + " TEXT,"
                + DBAlert.DETAILS_COL + " TEXT,"
                + DBAlert.CREATOR_COL + " TEXT,"
                + DBAlert.TYPE_COL + " TEXT,"
                + DBAlert.ATTACHMENT_URLS_COL + " TEXT,"
                + DBAlert.IS_DETAILED_COL + " BOOLEAN,"
                + DBAlert.ALERT_ID + " INTEGER"+")";

        db.execSQL(query);
//endregion

        //region Crea tabella con nome absence con le colonne specificate
        String query2 = "CREATE TABLE " + ABSENCES_TABLE + " ("
                + DBAbsece.DATE_COL + " TEXT,"
                + DBAbsece.TYPE_COL + " TEXT,"
                + DBAbsece.NOTES_COL+" TEXT,"
                +DBAbsece.IS_JUSTIFIED_COL+" BOOLEAN,"
                +DBAbsece.IS_MODIFICABLE_COL+" BOOLEAN,"
                +DBAbsece.JUSTIFY_URL_COL+" TEXT"+")";
        db.execSQL(query2);
//endregion

        //region Crea tabella con nome activity con le colonne specificate
        String query3 = "CREATE TABLE " + ACTIVITIES_TABLE + " ("
                + DBActivity.DATE_COL + " TEXT,"
                + DBActivity.CREATOR_COL + " TEXT,"
                + DBActivity.DETAILS_COL + " TEXT,"
                +DBActivity.EXISTS_COL+" BOOLEAN"+")";
        db.execSQL(query3);
//endregion

        //regionCrea tabella con nome authorization con le colonne specificate
        String query4="CREATE TABLE "+ AUTHORIZATIONS_TABLE +" ("
                + DBAuthorization.ENTRY_COL+" TEXT,"
                +DBAuthorization.EXIT_COL+" TEXT"+")";
        db.execSQL(query4);
//endregion

        //region Crea tabella con nome disciplinaryNote con le colonne specificate
        String query5="CREATE TABLE "+DISCIPLINARY_NOTICES_TABLE+" ("
                +DBDisciplinaryNote.DATE_COL+" TEXT,"
                +DBDisciplinaryNote.TYPE_COL+" TEXT,"
                +DBDisciplinaryNote.DETAILS_COL+" TEXT,"
                +DBDisciplinaryNote.COUNTERMEASURES_COL+" TEXT,"
                +DBDisciplinaryNote.AUTHOR_OF_DETAILS_COL+" TEXT,"
                +DBDisciplinaryNote.AUTHOR_OF_COUNTERMEASURES_COL+" TEXT,"
                +DBDisciplinaryNote.QUARTERLY_COL+" TEXT"+")";
        db.execSQL(query5);
        //endregion

        //region Crea tabella con nome homework con le colonne specificate
        String query6 = "CREATE TABLE " + HOMEWORKS_TABLE + " ("
                + DBHomework.DATE_COL + " TEXT,"
                + DBHomework.SUBJECT_COL+" TEXT,"
                + DBHomework.CREATOR_COL + " TEXT,"
                + DBHomework.DETAILS_COL + " TEXT,"
                + DBHomework.EXISTS_COL+" BOOLEAN"+")";
        db.execSQL(query6);
        //endregion

        //region Crea tabella con nome test con le colonne specificate
        String query7 = "CREATE TABLE " + TESTS_TABLE + " ("
                + DBTest.DATE_COL + " TEXT,"
                + DBTest.SUBJECT_COL+" TEXT,"
                + DBTest.CREATOR_COL + " TEXT,"
                + DBTest.DETAILS_COL + " TEXT,"
                +DBTest.EXISTS_COL+" BOOLEAN"+")";
        db.execSQL(query7);
        //endregion
    }



    //region DB Alert
    private long addAlert(Alert alert, SQLiteDatabase db){
        ContentValues values = new ContentValues();

        values.put(DBAlert.STATUS_COL, alert.status);
        values.put(DBAlert.DATE_COL, alert.date);
        values.put(DBAlert.RECEIVERS_COL, alert.receivers);
        values.put(DBAlert.OBJECT_COL, alert.object);
        values.put(DBAlert.PAGE_COL, alert.page);
        values.put(DBAlert.DETAILS_URL_COL, alert.detailsUrl);
        values.put(DBAlert.DETAILS_COL, alert.details);
        values.put(DBAlert.CREATOR_COL, alert.creator);
        values.put(DBAlert.TYPE_COL, alert.type);

        //Non si può memorizzare una lista su sql
        String attachmentUrls = null;
        if(alert.attachmentUrls != null){
            for(String url : alert.attachmentUrls){
                attachmentUrls += url + ";";
            }
        }
        values.put(DBAlert.ATTACHMENT_URLS_COL, attachmentUrls);
        values.put(DBAlert.IS_DETAILED_COL, alert.isDetailed ? 1 : 0); //false = 0, true = 1

        String[] a = alert.detailsUrl.split("/");
        int id = Integer.parseInt(a[a.length -1]);

        values.put(DBAlert.ALERT_ID, id);

        long b = db.insert(ALERTS_TABLE, null, values);

        return b;
    }

    public void addAlerts(List<Alert> alerts){
        SQLiteDatabase db = getWritableDatabase();

        for (Alert alert : alerts) {
            addAlert(alert, db);
        }

        db.close();
    }

    public List<Alert> readAlerts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + ALERTS_TABLE + " ORDER BY " + DBAlert.ALERT_ID + " DESC", null);

        List<Alert> alerts = new Vector<>();

        if (cursor.moveToFirst()) {
            do {
                boolean isDetailed = cursor.getInt(10) != 0; //0 = false, 1 = true

                if(isDetailed){
                    List<String> attachmentUrls = null;
                    try{
                        attachmentUrls = Arrays.asList(cursor.getString(6).split(";"));
                    } catch(NullPointerException ignored){ }

                    alerts.add(new Alert(cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getInt(5),
                            attachmentUrls,
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getString(9)));

                } else {
                    alerts.add(new Alert(cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(5),
                            cursor.getInt(4)));
                }


            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return alerts;
    }

    //Identificativi delle colonne di Alert
    private static class DBAlert {
        private static final String STATUS_COL = "status";
        private static final String DATE_COL = "date";
        private static final String RECEIVERS_COL = "receivers";
        private static final String OBJECT_COL = "object";
        private static final String PAGE_COL = "page";
        private static final String DETAILS_URL_COL = "detailsUrl";
        private static final String DETAILS_COL = "details";
        private static final String CREATOR_COL = "creator";
        private static final String TYPE_COL = "type";
        private static final String ATTACHMENT_URLS_COL = "attachmentUrls";
        private static final String IS_DETAILED_COL = "isDetailed";

        private static final String ALERT_ID = "id";
    }
    //endregion

    //region DB Absence
    private long addAbsence(Absence absence, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBAbsece.DATE_COL, absence.date);
        values.put(DBAbsece.TYPE_COL, absence.type);
        values.put(DBAbsece.NOTES_COL, absence.notes);
        values.put(DBAbsece.IS_JUSTIFIED_COL, absence.isJustified);
        values.put(DBAbsece.IS_JUSTIFIED_COL, absence.isModificable);
        values.put(DBAbsece.JUSTIFY_URL_COL, absence.justifyUrl);

        return db.insert(ABSENCES_TABLE, null, values);
    }

    public void addAbsences(List<Absence> absences){
        SQLiteDatabase db = getWritableDatabase();

        for (Absence absence : absences) {
            addAbsence(absence, db);
        }

        db.close();
    }

    public List<Absence> readAbsences() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + ABSENCES_TABLE, null);

        List<Absence> absences = new Vector<>();

        if (cursor.moveToFirst()) {
            boolean isJustify=true;
            if(cursor.getInt(3)==0) isJustify=false;
            boolean isModificable=true;
            if(cursor.getInt(4)==0) isModificable=false;
            do {
                absences.add(new Absence(cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        isJustify,
                        isModificable,
                        cursor.getString(5)));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return absences;
    }

    //Identificativi delle colonne di Absence
    private static class DBAbsece {
        private static final String DATE_COL = "date";
        private static final String TYPE_COL = "type";
        private static final String NOTES_COL = "notes";
        private static final String IS_JUSTIFIED_COL = "isJustified";
        private static final String IS_MODIFICABLE_COL = "isModificable";
        private static final String JUSTIFY_URL_COL = "justifyUrl";
    }
    //endregion

    //region Agenda Objects

    //region DB Activity
    private long addActivity(Activity activity, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBActivity.DATE_COL, activity.date);
        values.put(DBActivity.CREATOR_COL, activity.creator);
        values.put(DBActivity.DETAILS_COL, activity.details);
        values.put(DBActivity.EXISTS_COL, activity._exists);

        return db.insert(ACTIVITIES_TABLE, null, values);
    }

    public void addActivities(List<Activity> activities){
        SQLiteDatabase db = getWritableDatabase();

        for (Activity activity : activities) {
            addActivity(activity, db);
        }

        db.close();
    }

    public List<Activity> readActivities() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + ACTIVITIES_TABLE, null);

        List<Activity> activities = new Vector<>();

        if (cursor.moveToFirst()) {
            boolean exists=true;
            if(cursor.getInt(3)==0) exists=false;
            do {
                activities.add(new Activity(cursor.getString(0).split("-")[2],
                        cursor.getString(0).split("-")[1],
                        cursor.getString(0).split("-")[0],
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        exists));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return activities;
    }

    //identificativi delle colonne di Activity
    private static class DBActivity{
        private static final String DATE_COL="date";
        private static final String CREATOR_COL="creator";
        private static final String DETAILS_COL="details";
        private static final String EXISTS_COL="_exists";
    }
    //endregion

    //region DBHomework

    private long addHomework(Homework homework, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBHomework.DATE_COL, homework.date);
        values.put(DBHomework.SUBJECT_COL, homework.subject);
        values.put(DBHomework.CREATOR_COL, homework.creator);
        values.put(DBHomework.DETAILS_COL, homework.details);
        values.put(DBHomework.EXISTS_COL, homework._exists);

        return db.insert(HOMEWORKS_TABLE, null, values);
    }

    public void addHomeworks(List<Homework> homeworks){
        SQLiteDatabase db = getWritableDatabase();

        for (Homework homework : homeworks) {
            addHomework(homework, db);
        }

        db.close();
    }

    public List<Homework> readHomeworks() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + HOMEWORKS_TABLE, null);

        List<Homework> homeworks = new Vector<>();

        if (cursor.moveToFirst()) {
            boolean exists=true;
            if(cursor.getInt(4)==0) exists=false;
            do {
                homeworks.add(new Homework(cursor.getString(0).split("-")[2],
                        cursor.getString(0).split("-")[1],
                        cursor.getString(0).split("-")[0],
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        exists));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return homeworks;
    }

    private static class DBHomework{
        private static final String SUBJECT_COL="subject";
        private static final String CREATOR_COL="creator";
        private static final String DETAILS_COL="details";
        private static final String EXISTS_COL="_exists";
        private static final String DATE_COL="date";
    }

    //endregion

    //region DBTest

    private long addTest(Test test, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBTest.DATE_COL, test.date);
        values.put(DBTest.SUBJECT_COL, test.subject);
        values.put(DBTest.CREATOR_COL, test.creator);
        values.put(DBTest.DETAILS_COL, test.details);
        values.put(DBTest.EXISTS_COL, test._exists);

        return db.insert(TESTS_TABLE, null, values);
    }

    public void addTests(List<Test> tests){
        SQLiteDatabase db = getWritableDatabase();

        for (Test test : tests) {
            addTest(test, db);
        }

        db.close();
    }

    public List<Test> readTests() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TESTS_TABLE, null);

        List<Test> tests = new Vector<>();

        if (cursor.moveToFirst()) {
            boolean exists=true;
            if(cursor.getInt(4)==0) exists=false;
            do {
                tests.add(new Test(cursor.getString(0).split("-")[2],
                        cursor.getString(0).split("-")[1],
                        cursor.getString(0).split("-")[0],
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        exists));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return tests;
    }

    private static class DBTest{
        private static final String SUBJECT_COL="subject";
        private static final String CREATOR_COL="creator";
        private static final String DETAILS_COL="details";
        private static final String EXISTS_COL="_exists";
        private static final String DATE_COL="date";
    }

    //endre
    //endregion

    //endregion

    //region DB Authorization
    private long addAuthorization(Authorization authorization, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBAuthorization.ENTRY_COL, authorization.entry);
        values.put(DBAuthorization.EXIT_COL, authorization.exit);

        return db.insert(AUTHORIZATIONS_TABLE, null, values);
    }

    public void addAuthorizations(List<Authorization> authorizations){
        SQLiteDatabase db = getWritableDatabase();

        for (Authorization authorization : authorizations) {
            addAuthorization(authorization, db);
        }

        db.close();
    }

    public List<Authorization> readAuthorization() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + AUTHORIZATIONS_TABLE, null);

        List<Authorization> authorizations = new Vector<>();

        if (cursor.moveToFirst()) {
            do {
                authorizations.add(new Authorization(
                        cursor.getString(0),
                        cursor.getString(1)));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return authorizations;
    }

    // identificativi delle colonne di Authorization
    private static class DBAuthorization{
        private static final String ENTRY_COL="entry";
        private static final String EXIT_COL="exit";
    }
    //endregion

    //region DBDisciplinaryNote
    private long addDisciplinaryNote(DisciplinaryNotices disciplinaryNote, SQLiteDatabase db){
        ContentValues values = new ContentValues();
        values.put(DBDisciplinaryNote.DATE_COL, disciplinaryNote.date);
        values.put(DBDisciplinaryNote.TYPE_COL, disciplinaryNote.type);
        values.put(DBDisciplinaryNote.DETAILS_COL, disciplinaryNote.details);
        values.put(DBDisciplinaryNote.COUNTERMEASURES_COL, disciplinaryNote.countermeasures);
        values.put(DBDisciplinaryNote.AUTHOR_OF_DETAILS_COL, disciplinaryNote.authorOfDetails);
        values.put(DBDisciplinaryNote.AUTHOR_OF_COUNTERMEASURES_COL, disciplinaryNote.authorOfCountermeasures);
        values.put(DBDisciplinaryNote.QUARTERLY_COL, disciplinaryNote.quarterly);

        return db.insert(DISCIPLINARY_NOTICES_TABLE, null, values);
    }

    public void addDisciplinaryNotices(List<DisciplinaryNotices> disciplinaryNotices){
        SQLiteDatabase db = getWritableDatabase();

        for (DisciplinaryNotices disciplinaryNote : disciplinaryNotices) {
            addDisciplinaryNote(disciplinaryNote, db);
        }

        db.close();
    }

    public List<DisciplinaryNotices> readDisciplinaryNotices() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DISCIPLINARY_NOTICES_TABLE, null);

        List<DisciplinaryNotices> disciplinaryNotices = new Vector<>();

        if (cursor.moveToFirst()) {
            do {
                disciplinaryNotices.add(new DisciplinaryNotices(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6)));
            } while (cursor.moveToNext());
            //muovi il cursore nella prossima riga
        }
        cursor.close();
        return disciplinaryNotices;
    }

    private static class DBDisciplinaryNote{
        private static final String DATE_COL="date";
        private static final String TYPE_COL="type";
        private static final String DETAILS_COL="details";
        private static final String COUNTERMEASURES_COL="countermeasures";
        private static final String AUTHOR_OF_DETAILS_COL="authorOfDetails";
        private static final String AUTHOR_OF_COUNTERMEASURES_COL="authorOfCountermeasures";
        private static final String QUARTERLY_COL="quarterly";
    }
    //endregion




    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Se c'è stato un aggiornamento del database, crea uno nuovo
        db.execSQL("DROP TABLE IF EXISTS " + ALERTS_TABLE);
        onCreate(db);
    }
}
