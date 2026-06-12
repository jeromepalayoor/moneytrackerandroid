package net.jpalayoor.moneytracker.ui.monthly;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.ui.home.TransactionAdapter;
import net.jpalayoor.moneytracker.ui.home.TransactionDetailDialog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthDetailFragment extends Fragment {

    private MonthlyViewModel viewModel;
    private String month;
    private List<Transaction> currentTransactions;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_month_detail, container, false);

        viewModel = new ViewModelProvider(this).get(MonthlyViewModel.class);

        if (getArguments() != null) {
            month = getArguments().getString("month");
        }

        root.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(root).popBackStack()
        );

        root.findViewById(R.id.btnExport).setOnClickListener(v -> exportCSV());

        TextView tvMonthName = root.findViewById(R.id.tvDetailMonthName);
        TextView tvTxCount = root.findViewById(R.id.tvDetailTxCount);
        TextView tvInflow = root.findViewById(R.id.tvDetailInflow);
        TextView tvOutflow = root.findViewById(R.id.tvDetailOutflow);
        TextView tvNet = root.findViewById(R.id.tvDetailNet);
        LinearLayout llBreakdown = root.findViewById(R.id.llCategoryBreakdown);

        tvMonthName.setText(formatMonthDisplay(month));

        RecyclerView rv = root.findViewById(R.id.rvDetailTransactions);
        View emptyState = root.findViewById(R.id.llDetailEmptyState);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        TransactionAdapter adapter = new TransactionAdapter();
        rv.setAdapter(adapter);

        adapter.setOnTransactionClickListener(transaction -> {
            TransactionDetailDialog dialog = new TransactionDetailDialog(transaction,
                    new TransactionDetailDialog.OnTransactionAction() {
                        @Override
                        public void onUpdate(Transaction t) {
                            viewModel.update(t);
                        }
                        @Override
                        public void onDelete(Transaction t) {
                            viewModel.delete(t);
                        }
                    });
            dialog.show(getChildFragmentManager(), "TransactionDetail");
        });

        viewModel.getTransactionsByMonth(month).observe(getViewLifecycleOwner(), transactions -> {
            currentTransactions = transactions;
            adapter.setTransactions(transactions);
            tvTxCount.setText(getString(R.string.transaction_count, transactions.size()));

            if (transactions.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }

            double inflow = 0, outflow = 0;
            for (Transaction t : transactions) {
                if (t.amount >= 0) inflow += t.amount;
                else outflow += t.amount;
            }
            double net = inflow + outflow;

            tvInflow.setText(String.format(Locale.US,
                    "$%.2f", inflow));
            tvOutflow.setText(String.format(Locale.US,
                    "$%.2f", Math.abs(outflow)));
            tvNet.setText(String.format(Locale.US,
                    "%s$%.2f", net >= 0 ? "+" : "-", Math.abs(net)));
            tvNet.setTextColor(net >= 0 ? 0xFF10B981 : 0xFFF43F5E);

            // Build category breakdown
            buildCategoryBreakdown(inflater, llBreakdown, transactions);
        });

        return root;
    }

    private void buildCategoryBreakdown(LayoutInflater inflater, LinearLayout container,
                                        List<Transaction> transactions) {
        container.removeAllViews();

        // group by category
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            String cat = t.category != null ? t.category : "Other";
            categoryTotals.merge(cat, t.amount, Double::sum);
        }

        if (categoryTotals.isEmpty()) return;

        // find max absolute value for bar scaling
        double maxAbs = 0;
        for (double val : categoryTotals.values()) {
            if (Math.abs(val) > maxAbs) maxAbs = Math.abs(val);
        }

        // sort by absolute amount descending
        List<Map.Entry<String, Double>> entries = new ArrayList<>(categoryTotals.entrySet());
        entries.sort((a, b) ->
                Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));

        double finalMaxAbs = maxAbs;
        for (Map.Entry<String, Double> entry : entries) {
            String cat = entry.getKey();
            double amount = entry.getValue();

            View itemView = inflater.inflate(R.layout.item_category_breakdown,
                    container, false);

            TextView tvCatName = itemView.findViewById(R.id.tvCatName);
            TextView tvCatAmount = itemView.findViewById(R.id.tvCatAmount);
            View viewBar = itemView.findViewById(R.id.viewCatBar);

            tvCatName.setText(cat);

            // set category amount and colour
            if (amount >= 0) {
                tvCatAmount.setText(String.format(Locale.US, "+$%.2f", amount));
                tvCatAmount.setTextColor(0xFF10B981);
                viewBar.setBackgroundColor(0xFF10B981);
            } else {
                tvCatAmount.setText(String.format(Locale.US, "-$%.2f", Math.abs(amount)));
                tvCatAmount.setTextColor(0xFFF43F5E);
                viewBar.setBackgroundColor(0xFFF43F5E);
            }

            // set bar width proportionally
            float ratio = finalMaxAbs > 0 ? (float)(Math.abs(amount) / finalMaxAbs) : 0f;
            viewBar.post(() -> {
                int parentWidth = ((View) viewBar.getParent()).getWidth();
                ViewGroup.LayoutParams params = viewBar.getLayoutParams();
                params.width = (int)(parentWidth * ratio);
                viewBar.setLayoutParams(params);
            });

            container.addView(itemView);
        }
    }

    private void exportCSV() {
        // if no transactions
        if (currentTransactions == null || currentTransactions.isEmpty()) {
            Toast.makeText(getContext(), "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "MoneyTracker_" + month + ".csv";
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
                for (Transaction t : currentTransactions) {
                    writer.println(String.format(Locale.US, "%s,%s,%s,%.2f,%s",
                            t.date,
                            t.name != null ? t.name.replace(",", " ") : "",
                            t.category != null ? t.category.replace(",", " ") : "",
                            t.amount,
                            t.note != null ? t.note.replace(",", " ") : ""));
                }
                writer.flush();
                writer.close();

                Toast.makeText(getContext(), "Exported to Downloads/" + fileName,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e("ExportError", "Cannot write to null output stream.");
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String formatMonthDisplay(String month) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "MMMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(month);
            if (date != null) {
                return outputFormat.format(date);
            }
            return month;
        } catch (Exception e) {
            return month;
        }
    }
}