package net.jpalayoor.moneytracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import net.jpalayoor.moneytracker.data.AppDatabase;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.ui.settings.SettingsFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MoneyTrackerWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateWidget(Context context,
                                    AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // tap widget to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // set month label immediately
        String currentMonth = new SimpleDateFormat("MMMM yyyy",
                Locale.getDefault()).format(new Date());
        views.setTextViewText(R.id.widget_month, currentMonth);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String monthKey = new SimpleDateFormat("yyyy-MM",
                        Locale.getDefault()).format(new Date());
                List<Transaction> transactions = AppDatabase.getInstance(context)
                        .transactionDao().getAllTransactionsSync();

                double inflow = 0, outflow = 0;
                for (Transaction t : transactions) {
                    if (t.date.startsWith(monthKey)) {
                        if (t.amount >= 0) inflow += t.amount;
                        else outflow += t.amount;
                    }
                }
                double net = inflow + outflow;
                double spent = Math.abs(outflow);

                // get budget from preferences
                SharedPreferences prefs = context.getSharedPreferences(
                        SettingsFragment.PREFS_NAME, 0);
                float budget = prefs.getFloat(SettingsFragment.KEY_BUDGET, 0f);

                // update text views
                views.setTextViewText(R.id.widget_inflow,
                        String.format(Locale.US, "↑$%.0f", inflow));
                views.setTextViewText(R.id.widget_outflow,
                        String.format(Locale.US, "↓$%.0f", spent));
                views.setTextViewText(R.id.widget_balance,
                        String.format(Locale.US, "%s$%.2f", net >= 0 ? "" : "-",
                                Math.abs(net)));

                // update progress bar + budget label
                if (budget > 0) {
                    int progress = (int) Math.min((spent / budget) * 100, 100);
                    views.setTextViewText(R.id.widget_budget,
                            String.format(Locale.US, "$%.0f / $%.0f (%d%%)",
                                    spent, budget, progress));

                    if (progress >= 80) {
                        views.setViewVisibility(
                                R.id.widget_progress_green, android.view.View.GONE);
                        views.setViewVisibility(
                                R.id.widget_progress_red, android.view.View.VISIBLE);
                        views.setProgressBar(
                                R.id.widget_progress_red, 100, progress, false);
                        views.setInt(
                                R.id.widget_budget, "setTextColor", 0xFFF43F5E);
                    } else {
                        views.setViewVisibility(
                                R.id.widget_progress_green, android.view.View.VISIBLE);
                        views.setViewVisibility(
                                R.id.widget_progress_red, android.view.View.GONE);
                        views.setProgressBar(
                                R.id.widget_progress_green, 100, progress, false);
                        views.setInt(
                                R.id.widget_budget, "setTextColor", 0xFF94A3B8);
                    }
                } else {
                    views.setViewVisibility(
                            R.id.widget_progress_green, android.view.View.VISIBLE);
                    views.setViewVisibility(
                            R.id.widget_progress_red, android.view.View.GONE);
                    views.setProgressBar(
                            R.id.widget_progress_green, 100, 0, false);
                    views.setTextViewText(
                            R.id.widget_budget, "No budget set");
                }

                appWidgetManager.updateAppWidget(appWidgetId, views);

            } catch (Exception e) {
                RemoteViews errorViews = new RemoteViews(
                        context.getPackageName(), R.layout.widget_layout);
                errorViews.setTextViewText(
                        R.id.widget_balance, "Tap to open");
                errorViews.setTextViewText(
                        R.id.widget_month, "Money Tracker");
                errorViews.setTextViewText(
                        R.id.widget_inflow, "");
                errorViews.setTextViewText(
                        R.id.widget_outflow, "");
                errorViews.setTextViewText(
                        R.id.widget_budget, "");
                appWidgetManager.updateAppWidget(appWidgetId, errorViews);
            }
        });
    }

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, MoneyTrackerWidget.class));
        if (appWidgetIds.length > 0) {
            new MoneyTrackerWidget().onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}