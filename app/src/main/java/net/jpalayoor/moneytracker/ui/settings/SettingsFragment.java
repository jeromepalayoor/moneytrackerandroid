package net.jpalayoor.moneytracker.ui.settings;

import android.Manifest;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.AppDatabase;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.utils.NotificationScheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private CategoryAdapter categoryAdapter;
    private List<String> categories;

    // init settings preferences
    public static final String PREFS_NAME = "MoneyTrackerPrefs";
    public static final String KEY_BUDGET = "monthly_budget";
    public static final String KEY_DEFAULT_TYPE = "default_type";
    public static final String KEY_NOTIFICATIONS = "notifications_enabled";
    public static final String KEY_FREQUENCY = "notification_frequency";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_BIOMETRIC = "biometric_lock";
    public static final String KEY_CATEGORIES = "categories";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                if (isGranted) {
                    // once permission is granted, safely kick off the scheduler
                    int savedFrequency = prefs.getInt(KEY_FREQUENCY, 0);
                    NotificationScheduler.schedule(requireContext(), savedFrequency);
                } else {
                    Toast.makeText(requireContext(),
                            "Permission denied. Notifications disabled.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        EditText etBudget = root.findViewById(R.id.etBudget);
        float savedBudget = prefs.getFloat(KEY_BUDGET, 0f);
        if (savedBudget > 0) etBudget.setText(String.valueOf(savedBudget));

        etBudget.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,
                                                    int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s,
                                                int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String budgetStr = s.toString().trim();
                if (!budgetStr.isEmpty()) {
                    try {
                        prefs.edit().putFloat(KEY_BUDGET, Float.parseFloat(budgetStr)).apply();
                    } catch (NumberFormatException ignored) {}
                } else {
                    prefs.edit().putFloat(KEY_BUDGET, 0f).apply();
                }
            }
        });

        // default transaction type
        Spinner spinnerDefaultType = root.findViewById(R.id.spinnerDefaultType);
        String[] types = {"Outflow", "Inflow"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDefaultType.setAdapter(typeAdapter);
        spinnerDefaultType.setSelection(prefs.getInt(KEY_DEFAULT_TYPE, 0));

        spinnerDefaultType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_DEFAULT_TYPE, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // notification
        SwitchMaterial switchNotifications = root.findViewById(R.id.switchNotifications);
        View rowNotificationFreq = root.findViewById(R.id.rowNotificationFreq);
        View dividerFreq = root.findViewById(R.id.dividerFreq);
        Spinner spinnerFrequency = root.findViewById(R.id.spinnerFrequency);

        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, false);
        switchNotifications.setChecked(notificationsEnabled);
        rowNotificationFreq.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);
        dividerFreq.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);

        String[] frequencies = {"Daily", "Every 2 days", "Weekly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, frequencies);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(freqAdapter);
        spinnerFrequency.setSelection(prefs.getInt(KEY_FREQUENCY, 0));

        // notification handler
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rowNotificationFreq.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            dividerFreq.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            if (isChecked) {
                checkNotificationPermission();
                int savedFrequency = prefs.getInt(KEY_FREQUENCY, 0);
                NotificationScheduler.schedule(requireContext(), savedFrequency);
            } else {
                NotificationScheduler.cancel(requireContext());
            }
        });

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_FREQUENCY, position).apply();
                // reschedule with new frequency if notifications are on
                if (prefs.getBoolean(KEY_NOTIFICATIONS, false)) {
                    NotificationScheduler.schedule(requireContext(), position);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // dark mode preference
        SwitchMaterial switchDarkMode = root.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
        });

        // biometric preference
        SwitchMaterial switchBiometric = root.findViewById(R.id.switchBiometric);
        switchBiometric.setChecked(prefs.getBoolean(KEY_BIOMETRIC, false));

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        boolean canUseBiometric = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS;

        if (!canUseBiometric) {
            switchBiometric.setEnabled(false);
            switchBiometric.setChecked(false);
        }

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> prefs
                .edit().putBoolean(KEY_BIOMETRIC, isChecked).apply());

        // categories preference
        RecyclerView rvCategories = root.findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        Set<String> savedCategories = prefs.getStringSet(KEY_CATEGORIES, null);
        if (savedCategories == null) {
            categories = new ArrayList<>(Arrays.asList("Pay", "Food", "Transport",
                    "Groceries", "Entertainment", "Misc", "Others"));
        } else {
            categories = new ArrayList<>(savedCategories);
        }

        categoryAdapter = new CategoryAdapter(categories, position -> {
            categories.remove(position);
            categoryAdapter.notifyItemRemoved(position);
            categoryAdapter.notifyItemRangeChanged(position, categories.size());
            prefs.edit().putStringSet(KEY_CATEGORIES, new HashSet<>(categories)).apply();
        });
        rvCategories.setAdapter(categoryAdapter);

        EditText etNewCategory = root.findViewById(R.id.etNewCategory);
        Button btnAddCategory = root.findViewById(R.id.btnAddCategory);

        btnAddCategory.setOnClickListener(v -> {
            String newCat = etNewCategory.getText().toString().trim();
            if (newCat.isEmpty()) {
                // if category name is empty
                Toast.makeText(getContext(),
                        "Please enter a category name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (categories.contains(newCat)) {
                // if category already exists
                Toast.makeText(getContext(),
                        "Category already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            categories.add(newCat);
            categoryAdapter.notifyItemInserted(categories.size() - 1);
            etNewCategory.setText("");
            prefs.edit().putStringSet(KEY_CATEGORIES, new HashSet<>(categories)).apply();
        });

        // export all transaction data function
        Button btnExportAll = root.findViewById(R.id.btnExportAll);
        btnExportAll.setOnClickListener(v -> exportAllTransactions());
        root.findViewById(R.id.rowAboutApp).setOnClickListener(v ->
                Navigation.findNavController(root)
                        .navigate(R.id.action_settings_to_about)
        );

        return root;
    }

    private void exportAllTransactions() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Transaction> all = AppDatabase.getInstance(requireContext())
                        .transactionDao().getAllTransactionsSync();

                if (all == null || all.isEmpty()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "No transactions to export",
                                    Toast.LENGTH_SHORT).show());
                    return;
                }

                String fileName = "MoneyTracker_All_" +
                        new SimpleDateFormat("yyyyMMdd",
                                Locale.getDefault()).format(new Date()) + ".csv";

                OutputStream outputStream;
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = requireContext().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("Failed to create MediaStore entry.");
                }
                outputStream = requireContext().getContentResolver().openOutputStream(uri);

                if (outputStream != null) {
                    PrintWriter writer = new PrintWriter(outputStream);
                    writer.println("Date,Name,Category,Amount,Note");
                    for (Transaction t : all) {
                        writer.println(String.format(Locale.US, "%s,%s,%s,%.2f,%s",
                                t.date,
                                t.name != null ? t.name.replace(",", " ") : "",
                                t.category != null ? t.category.replace(",", " ") : "",
                                t.amount,
                                t.note != null ? t.note.replace(",", " ") : ""));
                    }
                    writer.flush();
                    writer.close();

                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Exported to Downloads/" + fileName,
                                    Toast.LENGTH_LONG).show());
                } else {
                    Log.e("ExportError", "Cannot write to null output stream.");
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Export failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }
}