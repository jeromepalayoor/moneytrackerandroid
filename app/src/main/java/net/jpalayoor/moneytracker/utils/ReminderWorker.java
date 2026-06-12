package net.jpalayoor.moneytracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.jpalayoor.moneytracker.data.AppDatabase;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.ui.settings.SettingsFragment;

import java.util.List;
import java.util.Locale;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.PREFS_NAME, 0);

        boolean notificationsEnabled = prefs.getBoolean(
                SettingsFragment.KEY_NOTIFICATIONS, false);
        if (!notificationsEnabled) return Result.success();

        // send reminder notification
        NotificationHelper.sendReminderNotification(context);

        // Also check budget while we're at it
        float budget = prefs.getFloat(SettingsFragment.KEY_BUDGET, 0f);
        if (budget > 0) {
            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", Locale.US)
                    .format(new java.util.Date());
            List<Transaction> transactions = AppDatabase.getInstance(context)
                    .transactionDao().getAllTransactionsSync();

            double outflow = 0;
            for (Transaction t : transactions) {
                if (t.amount < 0 && t.date.startsWith(currentMonth)) {
                    outflow += Math.abs(t.amount);
                }
            }

            double percentage = (outflow / budget) * 100;
            if (percentage >= 80) {
                NotificationHelper.sendBudgetWarningNotification(context, outflow, budget);
            }
        }

        return Result.success();
    }
}