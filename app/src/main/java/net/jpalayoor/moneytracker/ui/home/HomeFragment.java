package net.jpalayoor.moneytracker.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.ui.settings.SettingsFragment;
import net.jpalayoor.moneytracker.MoneyTrackerWidget;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // recent transaction list recycler view setup
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        TransactionAdapter adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        // handle click on recent transaction and open detail dialog
        adapter.setOnTransactionClickListener(transaction -> {
            TransactionDetailDialog dialog = new TransactionDetailDialog(transaction,
                    new TransactionDetailDialog.OnTransactionAction() {
                        @Override
                        public void onUpdate(Transaction t) {
                            homeViewModel.update(t);
                            // update home screen widget if any
                            MoneyTrackerWidget.refreshAllWidgets(requireContext());
                        }
                        @Override
                        public void onDelete(Transaction t) {
                            homeViewModel.delete(t);
                            // update home screen widget if any
                            MoneyTrackerWidget.refreshAllWidgets(requireContext());
                        }
                    });
            dialog.show(getChildFragmentManager(), "TransactionDetail");
        });

        // get text views that require updates
        TextView tvInflow = root.findViewById(R.id.tvInflow);
        TextView tvOutflow = root.findViewById(R.id.tvOutflow);
        TextView tvBalance = root.findViewById(R.id.tvBalance);
        TextView tvBudgetLabel = root.findViewById(R.id.tvBudgetLabel);
        ProgressBar progressBudget = root.findViewById(R.id.progressBudget);

        // observe db and refresh view if transactions updated
        homeViewModel.transactions.observe(getViewLifecycleOwner(), adapter::setTransactions);

        // observe db for inflow
        homeViewModel.monthlyInflow.observe(getViewLifecycleOwner(), inflow -> {
            double amount = inflow != null ? inflow : 0;
            tvInflow.setText(String.format(java.util.Locale.US, "$%.2f", amount));
        });

        // observe db for outflow and update budget bar
        homeViewModel.monthlyOutflow.observe(getViewLifecycleOwner(), outflow -> {
            double amount = outflow != null ? Math.abs(outflow) : 0;
            tvOutflow.setText(String.format(java.util.Locale.US, "$%.2f", amount));

            // get budget value
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(SettingsFragment.PREFS_NAME, 0);
            float budget = prefs.getFloat(SettingsFragment.KEY_BUDGET, 0f);

            // handle if budget is set
            if (budget > 0) {
                // calculate progress percentage value
                int progress = (int) Math.min((amount / budget) * 100, 100);
                progressBudget.setProgress(progress);
                tvBudgetLabel.setText(String.format(java.util.Locale.US,
                        "$%.0f / $%.0f", amount, budget));

                if (progress >= 80) {
                    // make progress bar red if over 80%
                    progressBudget.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE24B4A));
                } else {
                    // make progress bar green if under 80%
                    progressBudget.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1D9E75));
                }
            } else {
                // no budget set
                tvBudgetLabel.setText(R.string.no_budget_set);
                progressBudget.setProgress(0);
            }
        });

        // calculate balance = inflow + outflow and update
        homeViewModel.monthlyInflow.observe(getViewLifecycleOwner(),
                inflow -> homeViewModel.monthlyOutflow.observe(getViewLifecycleOwner(), outflow -> {
            double in = inflow != null ? inflow : 0;
            double out = outflow != null ? outflow : 0;
            tvBalance.setText(String.format(java.util.Locale.US,
                    "$%.2f", in + out));
        }));

        // handle view for empty transaction list
        homeViewModel.transactions.observe(getViewLifecycleOwner(), transactions -> {
            adapter.setTransactions(transactions);

            View emptyState = root.findViewById(R.id.llEmptyState);
            if (transactions == null || transactions.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // + button -> add detail dialog
        FloatingActionButton fab = root.findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            // load preference of either inflow or outflow
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(SettingsFragment.PREFS_NAME, 0);
            // 0=outflow, 1=inflow
            int defaultType = prefs.getInt(SettingsFragment.KEY_DEFAULT_TYPE, 0);

            // load categories from preferences
            java.util.Set<String> savedCategories = prefs
                    .getStringSet(SettingsFragment.KEY_CATEGORIES, null);
            String[] categories;
            if (savedCategories != null && !savedCategories.isEmpty()) {
                // custom set categories
                java.util.List<String> catList = new java.util.ArrayList<>(savedCategories);
                java.util.Collections.sort(catList);
                catList.add(0, "Select category...");
                catList.add("Others");
                categories = catList.toArray(new String[0]);
            } else {
                // default categories if category list empty
                categories = new String[]{"Select category...", "Pay", "Food", "Transport",
                        "Groceries", "Entertainment", "Misc", "Others"};
            }

            // create the pop up to add new transaction details
            AddTransactionDialog dialog = new AddTransactionDialog(defaultType,
                    categories, transaction -> {
                homeViewModel.insert(transaction);
                MoneyTrackerWidget.refreshAllWidgets(requireContext());
            });
            dialog.show(getChildFragmentManager(), "AddTransaction");
        });

        // get current month and set the month label
        TextView tvMonth = root.findViewById(R.id.tvMonth);
        String currentMonth = new java.text.SimpleDateFormat("MMMM yyyy",
                java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvMonth.setText(currentMonth);

        return root;
    }
}