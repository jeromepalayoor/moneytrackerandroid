package net.jpalayoor.moneytracker.ui.home;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import net.jpalayoor.moneytracker.R;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.ui.settings.SettingsFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TransactionDetailDialog extends DialogFragment {

    public interface OnTransactionAction {
        void onUpdate(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    // Removed 'final' to allow the default constructor
    private Transaction transaction;
    private OnTransactionAction listener;

    // Required empty public constructor for system fragment recreation
    public TransactionDetailDialog() {
    }

    public TransactionDetailDialog(Transaction transaction, OnTransactionAction listener) {
        this.transaction = transaction;
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (transaction == null) {
            dismiss();
            return new View(requireContext());
        }

        View view = inflater.inflate(R.layout.dialog_transaction_detail,
                container, false);

        EditText etName = view.findViewById(R.id.etName);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etNote = view.findViewById(R.id.etNote);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etOtherCategory = view.findViewById(R.id.etOtherCategory);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        RadioGroup rgType = view.findViewById(R.id.rgType);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // fill data
        etName.setText(transaction.name);
        etAmount.setText(String.valueOf(Math.abs(transaction.amount)));
        etNote.setText(transaction.note);
        etDate.setText(transaction.date);
        etDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));

        // set inflow/outflow
        if (transaction.amount >= 0) {
            rgType.check(R.id.rbInflow);
        } else {
            rgType.check(R.id.rbOutflow);
        }

        // load categories preferences from settings
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(SettingsFragment.PREFS_NAME, 0);
        Set<String> savedCategories = prefs
                .getStringSet(SettingsFragment.KEY_CATEGORIES, null);
        String[] categories;
        if (savedCategories != null && !savedCategories.isEmpty()) {
            List<String> catList = new ArrayList<>(savedCategories);
            Collections.sort(catList);
            catList.add("Others");
            categories = catList.toArray(new String[0]);
        } else {
            categories = new String[]{"Pay", "Food", "Transport",
                    "Groceries", "Entertainment", "Misc", "Others"};
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, categories) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                return super.getDropDownView(position, convertView, parent);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // match existing category
        boolean foundCategory = false;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(transaction.category)) {
                spinnerCategory.setSelection(i);
                foundCategory = true;
                break;
            }
        }

        // if do not match existing category, put it in others
        if (!foundCategory) {
            spinnerCategory.setSelection(categories.length - 1);
            etOtherCategory.setVisibility(View.VISIBLE);
            etOtherCategory.setText(transaction.category);
        }

        // category picker handler
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(
                            ContextCompat.getColor(parent.getContext(), R.color.primary_text));
                }
                etOtherCategory.setVisibility(
                        categories[position].equals("Others") ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // date picker handler
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), R.style.CustomDatePickerTheme,
                    (datePicker, year, month, day) -> {
                        String date = String.format(java.util.Locale.US ,
                                "%04d-%02d-%02d", year, month + 1, day);
                        etDate.setText(date);
                        etDate.setTextColor(ContextCompat.getColor(requireContext(),
                                R.color.primary_text));
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // save button handler
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            // check if category is set
            if (category.equals("Others")) {
                category = etOtherCategory.getText().toString().trim();
                if (category.isEmpty()) {
                    Toast.makeText(getContext(),
                            "Please specify category", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // check if name and amount is set
            if (name.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(),
                        "Name and amount are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // get amount
            double amount = Double.parseDouble(amountStr);
            if (rgType.getCheckedRadioButtonId() == R.id.rbOutflow) amount = -amount;

            transaction.name = name;
            transaction.amount = amount;
            transaction.category = category;
            transaction.note = etNote.getText().toString().trim();
            transaction.date = etDate.getText().toString().trim();

            // update db
            listener.onUpdate(transaction);
            dismiss();
        });

        // delete button handler
        btnDelete.setOnClickListener(v -> {
            // update db
            listener.onDelete(transaction);
            dismiss();
        });

        // cancel button handler
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}