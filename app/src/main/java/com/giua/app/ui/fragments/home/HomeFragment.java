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

package com.giua.app.ui.fragments.home;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.data.Entry;
import com.giua.app.AppUpdateManager;
import com.giua.app.AppUtils;
import com.giua.app.GlobalVariables;
import com.giua.app.IGiuaAppFragment;
import com.giua.app.LoggerManager;
import com.giua.app.OfflineDBController;
import com.giua.app.R;
import com.giua.app.SettingKey;
import com.giua.app.SettingsData;
import com.giua.app.ui.activities.DrawerActivity;
import com.giua.app.ui.fragments.lessons.LessonView;
import com.giua.app.ui.views.ObscureLayoutView;
import com.giua.objects.Lesson;
import com.giua.objects.Vote;
import com.giua.pages.LessonsPage;
import com.giua.pages.VotesPage;
import com.giua.webscraper.GiuaScraperExceptions;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

public class HomeFragment extends Fragment implements IGiuaAppFragment {

    LinearLayout contentLayout;
    TextView tvHomeworks;
    TextView tvTests;
    SwipeRefreshLayout swipeRefreshLayout;
    TextView tvUserInfo;
    ObscureLayoutView obscureLayoutView;
    LinearLayout lessonsVisualizerLayout;
    TextView tvLessonVisualizerArguments;
    TextView tvLessonVisualizerActivities;
    TextView tvLessonDate;

    Calendar calendar;
    Date currentDate;
    Date todayDate;
    Date yesterdayDate;
    Date tomorrowDate;
    SimpleDateFormat formatterForScraping = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN);
    SimpleDateFormat formatterForVisualize = new SimpleDateFormat("dd-MM-yyyy", Locale.ITALIAN);

    View root;
    TextView tvLessonVisualizerSupport;
    LoggerManager loggerManager;
    Activity activity;

    boolean forceRefresh = false;
    boolean isFragmentDestroyed = false;
    boolean offlineMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);

        activity = requireActivity();
        loggerManager = new LoggerManager("HomeFragment", activity);

        contentLayout = root.findViewById(R.id.home_content_layout);
        tvHomeworks = root.findViewById(R.id.home_txt_homeworks);
        tvTests = root.findViewById(R.id.home_txt_tests);
        swipeRefreshLayout = root.findViewById(R.id.home_swipe_refresh_layout);
        tvUserInfo = root.findViewById(R.id.home_user_info);
        obscureLayoutView = root.findViewById(R.id.home_obscure_view);

        lessonsVisualizerLayout = root.findViewById(R.id.home_lessons_visualizer_layout);
        tvLessonVisualizerArguments = root.findViewById((R.id.home_lessons_visualizer_arguments));
        tvLessonVisualizerActivities = root.findViewById((R.id.home_lessons_visualizer_activities));
        tvLessonVisualizerSupport = root.findViewById(R.id.home_lessons_visualizer_support);
        tvLessonDate = root.findViewById(R.id.home_txt_lessons_date);

        calendar = Calendar.getInstance();
        currentDate = new Date();
        todayDate = currentDate;
        yesterdayDate = getPrevDate(currentDate);
        tomorrowDate = getNextDate(currentDate);

        obscureLayoutView.setOnClickListener(this::obscureLayoutOnClick);
        root.findViewById(R.id.home_agenda_alerts).setOnClickListener(this::agendaAlertsOnClick);
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        offlineMode = activity.getIntent().getBooleanExtra("offline", false);

        tvLessonDate.setText("Oggi");
        root.findViewById(R.id.home_btn_lessons_next_date).setOnClickListener(this::nextDateOnClick);
        root.findViewById(R.id.home_btn_lessons_prev_date).setOnClickListener(this::prevDateOnClick);

        new Thread(() -> {

            if (offlineMode) {
                activity.runOnUiThread(() -> tvUserInfo.setText("Accesso eseguito in modalità Offline"));
            } else {
                String userType = GlobalVariables.gS.getUserTypeString();
                activity.runOnUiThread(() -> tvUserInfo.setText("Accesso eseguito nell'account " + GlobalVariables.gS.getUser() + " (" + userType + ")"));
            }

            AppUpdateManager manager = new AppUpdateManager(activity);
            if (manager.checkForUpdates()) {
                activity.runOnUiThread(() -> {
                    loggerManager.d("Rendo visibile avviso su home dell'aggiornamento");
                    root.findViewById(R.id.home_app_update_reminder).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.home_app_update_reminder).setOnClickListener(this::updateReminderOnClick);
                });
            }
        }).start();

        return root;
    }

    private void obscureLayoutOnClick(View view) {
        if (lessonsVisualizerLayout.getVisibility() == View.VISIBLE)
            lessonsVisualizerLayout.startAnimation(AnimationUtils.loadAnimation(requireActivity(), R.anim.visualizer_hide_effect));
        tvLessonVisualizerSupport.setVisibility(View.GONE);
        lessonsVisualizerLayout.setVisibility(View.GONE);
        obscureLayoutView.hide();
    }

    @Override
    public void loadOfflineDataAndViews() {
        /*new Thread(() -> {
            try {
                Map<String, List<Vote>> allVotes = new OfflineDBController(activity).readVotes();
                int homeworks = 0;
                int tests = 0;

                if (isFragmentDestroyed)
                    return;

                activity.runOnUiThread(() -> {
                    setupHomeworksTestsText(homeworks, tests);
                    if (!allVotes.isEmpty()) {
                        allCharts.get(0).refreshData(
                                activity,
                                "Andamento generale",
                                getMeanOfAllVotes(allVotes),
                                generateEntries(allVotes),
                                Arrays.asList(
                                        "Primo Quadrimestre",
                                        "Secondo Quadrimestre",
                                        "Terzo Quadrimestre"
                                ),
                                Arrays.asList(
                                        QUARTERLY_COLORS[0],
                                        QUARTERLY_COLORS[1],
                                        QUARTERLY_COLORS[2]
                                ));
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (IllegalStateException ignored) {
                //Si verifica quando questa schermata è stata distrutta ma il thread cerca comunque di fare qualcosa
            }
        }).start();*/
        /*tvNoElements.setText("Non disponibile offline");
        tvNoElements.setVisibility(View.VISIBLE);
        otherInfoLayoutButton.setVisibility(View.INVISIBLE);
        root.findViewById(R.id.absences_other_info_number_absences).setVisibility(View.INVISIBLE);
        root.findViewById(R.id.absences_other_info_total_absences_time).setVisibility(View.INVISIBLE);
        swipeRefreshLayout.setRefreshing(false);*/
    }

    @Override
    public void loadDataAndViews() {
        GlobalVariables.gsThread.addTask(() -> {
            try {
                VotesPage votesPage = GlobalVariables.gS.getVotesPage(forceRefresh);
                LessonsPage lessonsPage = GlobalVariables.gS.getLessonsPage(forceRefresh);
                Map<String, List<Vote>> allVotes = votesPage.getAllVotes();
                List<Lesson> allLessons = lessonsPage.getAllLessonsFromDate(currentDate);
                int homeworks = GlobalVariables.gS.getHomePage(forceRefresh).getNearHomeworks();
                int tests = GlobalVariables.gS.getHomePage(false).getNearTests();

                new OfflineDBController(activity).addVotes(allVotes);

                if (forceRefresh)
                    forceRefresh = false;

                if (isFragmentDestroyed) return;

                activity.runOnUiThread(() -> {
                    setupHomeworksTestsText(homeworks, tests);
                    //if (!allVotes.isEmpty()) //TODO: ????????
                    refreshLessons(allLessons);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (GiuaScraperExceptions.YourConnectionProblems e) {
                activity.runOnUiThread(() -> {
                    setErrorMessage(activity.getString(R.string.your_connection_error), root);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (GiuaScraperExceptions.SiteConnectionProblems e) {
                activity.runOnUiThread(() -> {
                    setErrorMessage(activity.getString(R.string.site_connection_error), root);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (GiuaScraperExceptions.MaintenanceIsActiveException e) {
                activity.runOnUiThread(() -> {
                    setErrorMessage(activity.getString(R.string.maintenance_is_active_error), root);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (GiuaScraperExceptions.NotLoggedIn e) {
                activity.runOnUiThread(() -> {
                    ((DrawerActivity) activity).startActivityManager();
                });
            } catch (IllegalStateException ignored) {
            }   //Si verifica quando questa schermata è stata distrutta ma il thread cerca comunque di fare qualcosa
        });
    }

    private void refreshLessons(List<Lesson> allLessons) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        contentLayout.removeViews(4, contentLayout.getChildCount() - 4);
        params.setMargins(20, 40, 20, 0);

        for (Lesson lesson : allLessons) {
            LessonView lessonView = new LessonView(requireActivity(), null, lesson);
            lessonView.setId(View.generateViewId());
            lessonView.setLayoutParams(params);
            lessonView.setOnClickListener(this::lessonViewOnClick);

            contentLayout.addView(lessonView);
        }
    }

    private void lessonViewOnClick(View view) {
        lessonsVisualizerLayout.startAnimation(AnimationUtils.loadAnimation(requireActivity(), R.anim.visualizer_show_effect));
        lessonsVisualizerLayout.setVisibility(View.VISIBLE);
        obscureLayoutView.show();

        if (!((LessonView) view).lesson.arguments.equals(""))
            tvLessonVisualizerArguments.setText(Html.fromHtml("<b>Argomenti:</b> " + ((LessonView) view).lesson.arguments, Html.FROM_HTML_MODE_COMPACT));
        else
            tvLessonVisualizerArguments.setText(Html.fromHtml("<b>Argomenti:</b> (Non specificati)", Html.FROM_HTML_MODE_COMPACT));

        if (!((LessonView) view).lesson.activities.equals(""))
            tvLessonVisualizerActivities.setText(Html.fromHtml("<b>Attività:</b> " + ((LessonView) view).lesson.activities, Html.FROM_HTML_MODE_COMPACT));
        else
            tvLessonVisualizerActivities.setText(Html.fromHtml("<b>Attività:</b> (Non specificata)", Html.FROM_HTML_MODE_COMPACT));

        if (!((LessonView) view).lesson.support.equals("")) {
            tvLessonVisualizerSupport.setText(Html.fromHtml("<b>Sostegno:</b> " + ((LessonView) view).lesson.support, Html.FROM_HTML_MODE_COMPACT));
            tvLessonVisualizerSupport.setVisibility(View.VISIBLE);
        }

    }

    private void prevDateOnClick(View view) {
        //data precedente
        currentDate = getPrevDate(currentDate);
        //calendarView.setDate(currentDate.getTime());
        //lessonsVisualizerLayout.removeAllViews();
        setTextWithNames();
        if (!offlineMode)
            loadDataAndViews();
        else
            loadOfflineDataAndViews();
    }

    private void nextDateOnClick(View view) {
        //prossimo giorno
        currentDate = getNextDate(currentDate);
        //calendarView.setDate(currentDate.getTime());
        //lessonsVisualizerLayout.removeAllViews();
        setTextWithNames();
        if (!offlineMode)
            loadDataAndViews();
        else
            loadOfflineDataAndViews();
    }


    private void setTextWithNames() {
        String s = formatterForVisualize.format(currentDate);
        if (s.equals(formatterForVisualize.format(todayDate)))
            tvLessonDate.setText("Oggi");
        else if (s.equals(formatterForVisualize.format(yesterdayDate)))
            tvLessonDate.setText("Ieri");
        else if (s.equals(formatterForVisualize.format(tomorrowDate)))
            tvLessonDate.setText("Domani");
        else
            tvLessonDate.setText(formatterForVisualize.format(currentDate));
    }

    private Date getCurrentDate(Date date) {
        calendar.setTime(date);
        return calendar.getTime();
    }

    private Date getNextDate(Date date) {
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return calendar.getTime();
    }

    private Date getPrevDate(Date date) {
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        return calendar.getTime();
    }


    @Override
    public void addViews() {
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private void agendaAlertsOnClick(View view) {
        ((DrawerActivity) activity).myFragmentManager.changeFragment(R.id.nav_agenda);
        ((DrawerActivity) activity).selectItemInDrawer(16);
    }

    private void updateReminderOnClick(View view) {
        loggerManager.d("Aggiornamento app richiesto dall'utente tramite Home");
        new Thread(() -> new AppUpdateManager(activity).startUpdateDialog()).start();
    }

    private void onRefresh() {
        forceRefresh = true;
        if (!offlineMode)
            loadDataAndViews();
        else
            loadOfflineDataAndViews();
    }

    private void setupHomeworksTestsText(int homeworks, int tests) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);

        root.findViewById(R.id.home_txt_homeworks).setVisibility(View.GONE);
        root.findViewById(R.id.home_txt_tests).setVisibility(View.GONE);
        root.findViewById(R.id.home_agenda_alerts).setBackgroundTintList(activity.getResources().getColorStateList(R.color.middle_vote, activity.getTheme()));

        if (homeworks == 0 && tests == 0) {
            root.findViewById(R.id.home_agenda_alerts).setVisibility(View.VISIBLE);
            root.findViewById(R.id.home_agenda_alerts).setBackgroundTintList(activity.getResources().getColorStateList(R.color.general_view_color, activity.getTheme()));
            tvHomeworks.setVisibility(View.VISIBLE);
            tvHomeworks.setText("Non sono presenti attività nei prossimi giorni");
        }

        if (homeworks == 1) {
            root.findViewById(R.id.home_agenda_alerts).setVisibility(View.VISIBLE);
            tvHomeworks.setVisibility(View.VISIBLE);
            tvHomeworks.setText("E' presente un compito per domani");
        } else if (homeworks > 1) {
            root.findViewById(R.id.home_agenda_alerts).setVisibility(View.VISIBLE);
            tvHomeworks.setVisibility(View.VISIBLE);
            tvHomeworks.setText("Sono presenti " + homeworks + " compiti per domani");
        }

        if (tests == 1) {
            root.findViewById(R.id.home_agenda_alerts).setVisibility(View.VISIBLE);
            tvTests.setVisibility(View.VISIBLE);
            tvTests.setText("E' presente una verifica nei prossimi giorni");
        } else if (tests > 1) {
            root.findViewById(R.id.home_agenda_alerts).setVisibility(View.VISIBLE);
            tvTests.setVisibility(View.VISIBLE);
            tvTests.setText("Sono presenti " + tests + " verifiche nei prossimi giorni");
        }

        if (homeworks <= 0)
            tvTests.setLayoutParams(params);

        tvHomeworks.setBackground(null);
        tvTests.setBackground(null);
        tvHomeworks.setMinWidth(0);
        tvTests.setMinWidth(0);
    }


    private int getNumberFromMonth(String month) {
        switch (month) {
            case "Gennaio":
                return 1;
            case "Febbraio":
                return 2;
            case "Marzo":
                return 3;
            case "Aprile":
                return 4;
            case "Maggio":
                return 5;
            case "Giugno":
                return 6;
            case "Luglio":
                return 7;
            case "Agosto":
                return 8;
            case "Settembre":
                return 9;
            case "Ottobre":
                return 10;
            case "Novembre":
                return 11;
            case "Dicembre":
                return 12;
        }
        loggerManager.e("E' stato rilevato un mese che non ho capito: " + month);
        return -1;
    }



    private void setErrorMessage(String message, View root) {
        if (!isFragmentDestroyed)
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        if (!offlineMode)
            loadDataAndViews();
        else
            loadOfflineDataAndViews();
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        isFragmentDestroyed = true;
        super.onDestroyView();
    }
}
