package net.jpalayoor.moneytracker.ui.home;

import android.app.DatePickerDialog;
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

import java.util.Calendar;

public class AddTransactionDialog extends DialogFragment {

    public interface OnTransactionAdded {
        void onAdded(Transaction transaction);
    }

    // 0 = outflow, 1 = inflow
    private final int defaultType;
    private final String[] categories;
    private final OnTransactionAdded listener;

    public AddTransactionDialog() {
        this.defaultType = 0;
        this.categories = new String[0];
        this.listener = null;
    }

    public AddTransactionDialog(int defaultType,
                                String[] categories, OnTransactionAdded listener) {
        this.defaultType = defaultType;
        this.categories = categories;
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (listener == null) {
            dismiss();
            return null;
        }

        View view = inflater.inflate(R.layout.dialog_add_transaction, container, false);

        EditText etName = view.findViewById(R.id.etName);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etNote = view.findViewById(R.id.etNote);
        EditText etDate = view.findViewById(R.id.etDate);
        EditText etOtherCategory = view.findViewById(R.id.etOtherCategory);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        RadioGroup rgType = view.findViewById(R.id.rgType);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // apply inflow or outflow from preferences
        if (defaultType == 1) {
            rgType.check(R.id.rbInflow);
        } else {
            rgType.check(R.id.rbOutflow);
        }

        etDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));

        // date picker handler
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), R.style.CustomDatePickerTheme,
                    (datePicker, year, month, day) -> {
                        String date = String.format(java.util.Locale.US,
                                "%04d-%02d-%02d", year, month + 1, day);
                        etDate.setText(date);
                        etDate.setTextColor(ContextCompat.getColor(
                                requireContext(), R.color.primary_text));
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // category picker — uses categories from settings preferences
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, categories) {
            @Override
            public boolean isEnabled(int position) {
                // disable placeholder
                return position != 0;
            }
            @Override
            public View getDropDownView(int position,
                                        View convertView, @NonNull ViewGroup parent) {
                return super.getDropDownView(position, convertView, parent);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // category picker handler
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    // set the correct text colour
                    int colorRes = (position == 0) ? R.color.hint_text : R.color.primary_text;
                    tv.setTextColor(ContextCompat.getColor(parent.getContext(), colorRes));
                }
                // show others input only if others option is selected
                boolean isOthers = categories[position].equals("Others");
                etOtherCategory.setVisibility(isOthers ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // save button handler
        btnSave.setOnClickListener(v -> {
            // get the inputs
            String name = etName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            // if date is not inputted, use current date
            if (date.isEmpty()) {
                Calendar cal = Calendar.getInstance();
                date = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH));
            }

            // check if category is selected
            String category = spinnerCategory.getSelectedItem().toString();
            if (category.equals("Select category...")) {
                Toast.makeText(requireContext(),
                        "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            // check if others input is filled
            if (category.equals("Others")) {
                category = etOtherCategory.getText().toString().trim();
                if (category.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "Please specify category", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // check if name and amount is filled
            if (name.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Name and amount are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // format amount
            double amount = Double.parseDouble(amountStr);
            if (rgType.getCheckedRadioButtonId() == R.id.rbOutflow) {
                amount = -amount;
            }

            Transaction transaction = new Transaction();
            transaction.name = name;
            transaction.amount = amount;
            transaction.category = category;
            transaction.note = note;
            transaction.date = date;

            // update db
            listener.onAdded(transaction);
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