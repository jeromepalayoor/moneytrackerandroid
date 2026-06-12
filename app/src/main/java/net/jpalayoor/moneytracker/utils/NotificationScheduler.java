package net.jpalayoor.moneytracker.utils;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final String WORK_TAG = "money_tracker_reminder";

    public static void schedule(Context context, int frequencyIndex) {
        // 0 = Daily, 1 = Every 2 days, 2 = Weekly
        long intervalHours;
        switch (frequencyIndex) {
            case 1: intervalHours = 48; break;
            case 2: intervalHours = 168; break;
            default: intervalHours = 24; break;
        }

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                intervalHours,
                TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG);
    }
}