package net.jpalayoor.moneytracker.ui.monthly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.MonthTotal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyFragment extends Fragment {

    private MonthSummaryAdapter adapter;
    private final Map<String, Double> inflowMap = new HashMap<>();
    private final Map<String, Double> outflowMap = new HashMap<>();
    private List<String> months = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_monthly, container, false);

        MonthlyViewModel viewModel = new ViewModelProvider(this).get(MonthlyViewModel.class);

        // RecyclerView setup
        RecyclerView rvMonths = root.findViewById(R.id.rvMonths);
        rvMonths.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MonthSummaryAdapter();
        rvMonths.setAdapter(adapter);

        // navigate to detail on month click
        adapter.setOnMonthClickListener(month -> {
            Bundle args = new Bundle();
            args.putString("month", month);
            Navigation.findNavController(root).navigate(R.id.action_monthly_to_monthDetail, args);
        });

        // bar chart setup
        BarChart barChart = root.findViewById(R.id.barChart);
        setupBarChart(barChart);

        // observe distinct months for updates
        viewModel.distinctMonths.observe(getViewLifecycleOwner(), monthList -> {
            if (monthList == null) return;
            months = monthList;
            adapter.setMonths(monthList);
            updateChart(barChart);
        });

        // observe inflow totals for updates
        viewModel.monthlyInflowTotals.observe(getViewLifecycleOwner(), totals -> {
            if (totals == null) return;
            inflowMap.clear();
            for (MonthTotal mt : totals) inflowMap.put(mt.month, mt.total);
            adapter.setInflowMap(inflowMap);
            updateChart(barChart);
        });

        // observe outflow totals for updates
        viewModel.monthlyOutflowTotals.observe(getViewLifecycleOwner(), totals -> {
            if (totals == null) return;
            outflowMap.clear();
            for (MonthTotal mt : totals) outflowMap.put(mt.month, mt.total);
            adapter.setOutflowMap(outflowMap);
            updateChart(barChart);
        });

        return root;
    }

    private void setupBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.getAxisRight().setEnabled(false);

        // style y axis
        chart.getAxisLeft().setTextColor(0xFF94A3B8);
        chart.getAxisLeft().setGridColor(0xFF1E293B);
        chart.getAxisLeft().setAxisLineColor(0xFF1E293B);
        chart.getAxisLeft().setLabelCount(4, false);

        // style x axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFF94A3B8);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // style bg
        chart.setBackgroundColor(0xFF1A1A2E);
        chart.setTouchEnabled(false);
    }

    private void updateChart(BarChart chart) {
        List<String> chartMonths = getLast6Months();
        if (chartMonths.isEmpty()) return;

        List<BarEntry> inflowEntries = new ArrayList<>();
        List<BarEntry> outflowEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < chartMonths.size(); i++) {
            String m = chartMonths.get(i);

            // fetch once and check for null safely
            Double inflowVal = inflowMap.get(m);
            float inflow = (inflowVal != null) ? inflowVal.floatValue() : 0f;

            Double outflowVal = outflowMap.get(m);
            float outflow = (outflowVal != null) ? (float) Math.abs(outflowVal) : 0f;

            inflowEntries.add(new BarEntry(i, inflow));
            outflowEntries.add(new BarEntry(i, outflow));
            labels.add(formatMonthShort(m));
        }

        // show inflow bars
        BarDataSet inflowSet = new BarDataSet(inflowEntries, "Inflow");
        inflowSet.setColor(0xFF10B981);
        inflowSet.setDrawValues(false);

        // show outflow bars
        BarDataSet outflowSet = new BarDataSet(outflowEntries, "Outflow");
        outflowSet.setColor(0xFFF43F5E);
        outflowSet.setDrawValues(false);

        // format bar graph
        BarData data = new BarData(inflowSet, outflowSet);
        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;
        data.setBarWidth(barWidth);

        chart.setData(data);
        chart.groupBars(0f, groupSpace, barSpace);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(chartMonths.size());

        chart.invalidate();
    }

    private List<String> getLast6Months() {
        // get last 6 months from our data for chart display
        List<String> result = new ArrayList<>();
        for (int i = Math.min(months.size(), 6) - 1; i >= 0; i--) {
            result.add(months.get(i));
        }
        return result;
    }

    private String formatMonthShort(String month) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "MMM", Locale.getDefault());

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