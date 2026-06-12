package net.jpalayoor.moneytracker.ui.home;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;

    private final List<Object> items = new ArrayList<>();

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    private OnTransactionClickListener clickListener;

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.clickListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setTransactions(List<Transaction> transactions) {
        items.clear();
        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // keep track of date of latest transaction
        String lastDate = null;
        for (Transaction t : transactions) {
            String formattedHeader = formatDateHeader(t.date);
            if (!formattedHeader.equals(lastDate)) {
                // add date header
                items.add(formattedHeader);
                lastDate = formattedHeader;
            }
            // add transaction
            items.add(t);
        }
        notifyDataSetChanged();
    }

    // format date header
    private String formatDateHeader(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "dd MMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvDateHeader.setText((String) items.get(position));
        } else {
            Transaction t = (Transaction) items.get(position);
            TransactionViewHolder tvh = (TransactionViewHolder) holder;
            tvh.tvName.setText(t.name);
            tvh.tvCategory.setText(t.category);
            tvh.tvAmount.setText(String.format(java.util.Locale.US,
                    "$%.2f", Math.abs(t.amount)));
            if (t.amount >= 0) {
                tvh.tvAmount.setTextColor(0xFF1D9E75);
            } else {
                tvh.tvAmount.setTextColor(0xFFE24B4A);
            }
            tvh.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTransactionClick(t);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;
        HeaderViewHolder(View view) {
            super(view);
            tvDateHeader = view.findViewById(R.id.tvDateHeader);
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvAmount;
        TransactionViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvName);
            tvCategory = view.findViewById(R.id.tvCategory);
            tvAmount = view.findViewById(R.id.tvAmount);
        }
    }
}