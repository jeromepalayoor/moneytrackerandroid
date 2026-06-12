package net.jpalayoor.moneytracker;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.jpalayoor.moneytracker.ui.settings.SettingsFragment;
import net.jpalayoor.moneytracker.databinding.ActivityMainBinding;
import net.jpalayoor.moneytracker.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private View coverView;
    private static boolean isAuthenticated = false;
    private static long backgroundedAt = 0;
    private static final long LOCK_TIMEOUT_MS = 30 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("MoneyTrackerPrefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);

        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        net.jpalayoor.moneytracker.databinding.ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        NotificationHelper.createNotificationChannels(this);

        androidx.navigation.fragment.NavHostFragment navHostFragment =
                (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_activity_main);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);

            binding.navView.setOnItemSelectedListener(item -> {
                NavigationUI.onNavDestinationSelected(item, navController);
                navController.popBackStack(item.getItemId(), false);
                return true;
            });
        }

        // check biometric configuration to immediately apply secure overlay flags if enabled
        SharedPreferences settingsPrefs = getSharedPreferences(
                SettingsFragment.PREFS_NAME, 0);
        boolean biometricEnabled = settingsPrefs.getBoolean(
                SettingsFragment.KEY_BIOMETRIC, false);

        if (biometricEnabled && !isAuthenticated) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
            setupCoverView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, 0);
        boolean biometricEnabled = prefs.getBoolean(SettingsFragment.KEY_BIOMETRIC, false);

        if (!biometricEnabled) return;

        // Check timeout
        if (isAuthenticated && backgroundedAt > 0) {
            long elapsed = System.currentTimeMillis() - backgroundedAt;
            if (elapsed >= LOCK_TIMEOUT_MS) {
                isAuthenticated = false;
                coverView = null;
            } else {
                // Still authenticated — clear secure flag so screenshots work
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                return;
            }
        }

        if (!isAuthenticated) {
            setupCoverView();
            showBiometricPrompt();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCoverView() {
        if (coverView == null) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setBackgroundColor(0xFF1a1a2e);

            // app icon
            android.widget.ImageView ivIcon = new android.widget.ImageView(this);
            ivIcon.setImageResource(R.mipmap.ic_launcher_round);
            int iconSize = (int)(120 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout
                    .LayoutParams(iconSize, iconSize);
            iconParams.setMargins(0, 0, 0, 40);
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            ivIcon.setLayoutParams(iconParams);
            layout.addView(ivIcon, 0);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(getString(R.string.app_name));
            tvTitle.setTextColor(0xFFFFFFFF);
            tvTitle.setTextSize(30);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setGravity(Gravity.CENTER);
            layout.addView(tvTitle);

            Button btnUnlock = new Button(this);
            btnUnlock.setText(getString(R.string.btn_unlock_biometric));
            btnUnlock.setAllCaps(false);
            btnUnlock.setTextSize(20);
            btnUnlock.setBackgroundColor(0xFF00ADB5); // Vibrant teal modern accent button
            btnUnlock.setTextColor(0xFFFFFFFF);
            btnUnlock.setOnClickListener(v -> showBiometricPrompt());

            android.graphics.drawable.GradientDrawable btnShape = new android
                    .graphics.drawable.GradientDrawable();
            btnShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            btnShape.setColor(0xFF00ADB5); // Vibrant teal modern accent color

            // Convert 8dp to pixels so the rounding looks identical on all screen densities
            float density = getResources().getDisplayMetrics().density;
            btnShape.setCornerRadius(8 * density); // Change 8 to higher/lower to adjust roundness

            btnUnlock.setBackground(btnShape);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, // Width = wrap_content
                    ViewGroup.LayoutParams.WRAP_CONTENT  // Height = wrap_content
            );
            btnParams.setMargins(0, 50, 0, 0);
            btnUnlock.setLayoutParams(btnParams);
            btnUnlock.setPadding(48, 0, 48, 0);
            layout.addView(btnUnlock);

            layout.setClickable(true);
            layout.setFocusable(true);
            // intercept all touches so nothing underneath can be tapped
            layout.setOnTouchListener((v, event) -> true);

            coverView = layout;
            addContentView(coverView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    private void showBiometricPrompt() {
        // lock screen if biometric is enabled
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Money Tracker")
                .setSubtitle("Use your fingerprint or device credentials to continue")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        isAuthenticated = true;

                        // Remove the overlay layout completely upon authorization
                        if (coverView != null && coverView.getParent() != null) {
                            ((ViewGroup) coverView.getParent()).removeView(coverView);
                            coverView = null;
                        }
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }

                    @Override
                    public void onAuthenticationError(
                            int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            return;
                        }

                        // close the app if a critical permanent biometric lockout occurs
                        if (errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                            finish();
                        }
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, 0);
        boolean biometricEnabled = prefs.getBoolean(SettingsFragment.KEY_BIOMETRIC, false);
        if (biometricEnabled && isAuthenticated) {
            backgroundedAt = System.currentTimeMillis();
            // re-secure when leaving app
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }
    }
}