package net.jpalayoor.moneytracker.ui.monthly;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.jpalayoor.moneytracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthSummaryAdapter extends RecyclerView.Adapter<MonthSummaryAdapter.ViewHolder> {

    private List<String> months = new ArrayList<>();
    private Map<String, Double> inflowMap = new HashMap<>();
    private Map<String, Double> outflowMap = new HashMap<>();

    public interface OnMonthClickListener {
        void onMonthClick(String month);
    }

    private OnMonthClickListener clickListener;

    public void setOnMonthClickListener(OnMonthClickListener listener) {
        this.clickListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMonths(List<String> months) {
        this.months = months;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setInflowMap(Map<String, Double> map) {
        this.inflowMap = map;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setOutflowMap(Map<String, Double> map) {
        this.outflowMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String month = months.get(position);

        // format "2026-05" -> "May 2026"
        String displayName = formatMonthDisplay(month);
        holder.tvMonthName.setText(displayName);

        // get inflow and outflow and calculate net for month
        Double inflowObj = inflowMap.get(month);
        double inflow = (inflowObj != null) ? inflowObj : 0.0;
        Double outflowObj = outflowMap.get(month);
        double outflow = (outflowObj != null) ? outflowObj : 0.0;
        double net = inflow + outflow;

        // set values for inflow, outflow and net
        holder.tvMonthInflow.setText(String.format(Locale.US,
                "↑ $%.2f", inflow));
        holder.tvMonthOutflow.setText(String.format(Locale.US,
                "↓ $%.2f", Math.abs(outflow)));
        holder.tvMonthNet.setText(String.format(Locale.US,
                "%s$%.2f", net >= 0 ? "+" : "-", Math.abs(net)));
        holder.tvMonthNet.setTextColor(net >= 0 ? 0xFF10B981 : 0xFFF43F5E);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onMonthClick(month);
        });
    }

    // format month display
    private String formatMonthDisplay(String month) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);

            Date date = inputFormat.parse(month);
            if (date != null) {
                return outputFormat.format(date);
            }
            return month;
        } catch (Exception e) {
            return month;
        }
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthName, tvMonthNet, tvMonthInflow, tvMonthOutflow;
        ViewHolder(View view) {
            super(view);
            tvMonthName = view.findViewById(R.id.tvMonthName);
            tvMonthNet = view.findViewById(R.id.tvMonthNet);
            tvMonthInflow = view.findViewById(R.id.tvMonthInflow);
            tvMonthOutflow = view.findViewById(R.id.tvMonthOutflow);
        }
    }
}