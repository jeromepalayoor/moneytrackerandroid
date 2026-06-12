package net.jpalayoor.moneytracker.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import net.jpalayoor.moneytracker.MainActivity;
import net.jpalayoor.moneytracker.R;

import java.util.Locale;

public class NotificationHelper {

    public static final String CHANNEL_ID_REMINDER = "reminder_channel";
    public static final String CHANNEL_ID_BUDGET = "budget_channel";

    public static void createNotificationChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);

        // reminder channel
        NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Spending Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        reminderChannel.setDescription("Periodic reminders to log your spending");
        manager.createNotificationChannel(reminderChannel);

        // budget warning channel
        NotificationChannel budgetChannel = new NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Budget Warnings",
                NotificationManager.IMPORTANCE_HIGH);
        budgetChannel.setDescription("Alerts when you're close to or over your budget");
        manager.createNotificationChannel(budgetChannel);
    }

    public static void sendReminderNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ID_REMINDER)
                .setSmallIcon(R.drawable.ic_home_black_24dp)
                .setContentTitle("Money Tracker")
                .setContentText("Don't forget to log your spending today!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == android
                .content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }

    public static void sendBudgetWarningNotification(Context context, double spent, double budget) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        boolean overBudget = spent >= budget;
        String title = overBudget ? "Over Budget!" : "Budget Warning";
        String message = overBudget
                ? String.format(Locale.US,
                "You've spent $%.0f — $%.0f over your $%.0f budget!",
                spent, spent - budget, budget)
                : String.format(Locale.US,
                "You've spent $%.0f of your $%.0f budget (%.0f%%)",
                spent, budget, (spent/budget)*100);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ID_BUDGET)
                .setSmallIcon(R.drawable.ic_home_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == android
                .content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build());
        }
    }
}