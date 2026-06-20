package com.example.chitfund;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long chitId = -1;

    private Spinner spChitSelector;
    private Spinner spMembers, spInstallmentOptions;
    private TableLayout tlFundTable;
    private TextView tvFundTitle;
    private LinearLayout llFormContainer;
    private int totalInstallmentsCount;
    private String frequencyType;
    private String firstInstallmentDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);

        spChitSelector = findViewById(R.id.spChitSelector);
        spMembers = findViewById(R.id.spMembers);
        spInstallmentOptions = findViewById(R.id.spInstallmentOptions);
        Button btnAddInstallment = findViewById(R.id.btnAddInstallment);
        tlFundTable = findViewById(R.id.tlFundTable);
        tvFundTitle = findViewById(R.id.tvFundTitle);
        llFormContainer = findViewById(R.id.llFormContainer);

        // Listen for user switching between different chit groups
        spChitSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DatabaseHelper.ChitItem selected = (DatabaseHelper.ChitItem) parent.getItemAtPosition(position);
                if (selected != null && selected.id != chitId) {
                    chitId = selected.id;
                    loadChitMetaData();
                    refreshFundMatrixTable();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load complete selector records on launch
        populateChitSelector(-1);

        btnAddInstallment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chitId == -1) {
                    Toast.makeText(MainActivity.this, "Please create a Chit Fund group first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (spMembers.getSelectedItem() == null || spInstallmentOptions.getSelectedItem() == null) return;
                
                String selectedMember = spMembers.getSelectedItem().toString();
                int instNum = spInstallmentOptions.getSelectedItemPosition() + 1;
                double currentTargetAmount = dbHelper.getInstallmentAmount(chitId, instNum);

                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                dbHelper.insertPayment(chitId, instNum, currentDate, selectedMember, currentTargetAmount);

                Toast.makeText(MainActivity.this, "Installment Saved!", Toast.LENGTH_SHORT).show();
                refreshFundMatrixTable();
            }
        });
    }

    private void populateChitSelector(long targetChitId) {
        ArrayList<DatabaseHelper.ChitItem> chits = dbHelper.getChitList();
        ArrayAdapter<DatabaseHelper.ChitItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, chits);
        spChitSelector.setAdapter(adapter);

        if (chits.isEmpty()) {
            chitId = -1;
            tvFundTitle.setText("No active Chit Fund found. Create one using the menu!");
            tlFundTable.removeAllViews();
            llFormContainer.setVisibility(View.GONE);
            return;
        }

        llFormContainer.setVisibility(View.VISIBLE);

        // Figure out which item position index matches our selection goal
        int selectIndex = 0;
        if (targetChitId != -1) {
            for (int i = 0; i < chits.size(); i++) {
                if (chits.get(i).id == targetChitId) {
                    selectIndex = i;
                    break;
                }
            }
        }
        
        spChitSelector.setSelection(selectIndex);
        
        // Explicitly trigger render pass updates for target focus
        DatabaseHelper.ChitItem selected = (DatabaseHelper.ChitItem) spChitSelector.getSelectedItem();
        if (selected != null) {
            chitId = selected.id;
            loadChitMetaData();
            refreshFundMatrixTable();
        }
    }

    private void loadChitMetaData() {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT name, frequency, installments, start_date FROM chits WHERE id = ?", new String[]{String.valueOf(chitId)});
        if (c.moveToFirst()) {
            String chitName = c.getString(0);
            frequencyType = c.getString(1);
            totalInstallmentsCount = c.getInt(2);
            firstInstallmentDateStr = c.getString(3);

            setTitle(chitName);
            tvFundTitle.setText("Chit Fund: " + chitName);

            ArrayList<String> members = dbHelper.getMembers(chitId);
            spMembers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, members));

            ArrayList<String> optionsList = new ArrayList<>();
            for (int i = 1; i <= totalInstallmentsCount; i++) {
                double amt = dbHelper.getInstallmentAmount(chitId, i);
                optionsList.add("Installment " + i + " - ₹" + amt);
            }
            spInstallmentOptions.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, optionsList));
        }
        c.close();
    }

    private void refreshFundMatrixTable() {
        tlFundTable.removeAllViews();
        if (chitId == -1) return;

        ArrayList<String> calculatedDatesHeaders = new ArrayList<>();
        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfOutput = new SimpleDateFormat("d - MMM - yy", Locale.getDefault());

        try {
            Date startDate = sdfInput.parse(firstInstallmentDateStr);
            Calendar cal = Calendar.getInstance();
            
            for (int i = 0; i < totalInstallmentsCount; i++) {
                cal.setTime(startDate);
                if (frequencyType.equals("Monthly")) {
                    cal.add(Calendar.MONTH, i);
                } else {
                    cal.add(Calendar.DATE, i * 7);
                }
                calculatedDatesHeaders.add(sdfOutput.format(cal.getTime()));
            }
        } catch (Exception e) {
            calculatedDatesHeaders.add(firstInstallmentDateStr);
        }

        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#E0E0E0"));
        headerRow.setPadding(6, 12, 6, 12);

        TextView hNo = new TextView(this); hNo.setText("No."); hNo.setPadding(12, 6, 12, 6); hNo.setTextSize(14); headerRow.addView(hNo);
        TextView hName = new TextView(this); hName.setText("Name"); hName.setPadding(12, 6, 12, 6); hName.setTextSize(14); headerRow.addView(hName);

        for (String dateStr : calculatedDatesHeaders) {
            TextView hDate = new TextView(this);
            hDate.setText(dateStr);
            hDate.setPadding(12, 6, 12, 6);
            hDate.setTextSize(14);
            headerRow.addView(hDate);
        }
        tlFundTable.addView(headerRow);

        ArrayList<String> totalMembersList = dbHelper.getMembers(chitId);
        int serialCounter = 1;

        for (String name : totalMembersList) {
            TableRow memberRow = new TableRow(this);
            memberRow.setPadding(6, 10, 6, 10);

            TextView tvSerial = new TextView(this); tvSerial.setText(String.valueOf(serialCounter++)); tvSerial.setPadding(12, 6, 12, 6); memberRow.addView(tvSerial);
            TextView tvName = new TextView(this); tvName.setText(name); tvName.setPadding(12, 6, 12, 6); memberRow.addView(tvName);

            for (int i = 1; i <= totalInstallmentsCount; i++) {
                TextView tvStatusCell = new TextView(this);
                tvStatusCell.setPadding(12, 6, 12, 6);
                
                if (dbHelper.isPaymentMade(chitId, name, i)) {
                    tvStatusCell.setText("✅");
                } else {
                    tvStatusCell.setText("");
                }
                memberRow.addView(tvStatusCell);
            }
            tlFundTable.addView(memberRow);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_new_chit) {
            showNewChitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNewChitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_new_chit, null);

        final EditText etChitName = view.findViewById(R.id.etChitName);
        final Spinner spFrequency = view.findViewById(R.id.spFrequency);
        final EditText etInstallmentsCount = view.findViewById(R.id.etInstallmentsCount);
        final Spinner spAmountType = view.findViewById(R.id.spAmountType);
        final EditText etAmount = view.findViewById(R.id.etAmount);
        final LinearLayout llAmountsContainer = view.findViewById(R.id.llAmountsContainer);
        final EditText etDate = view.findViewById(R.id.etDate);
        final LinearLayout llMembersContainer = view.findViewById(R.id.llMembersContainer);

        spFrequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Monthly", "Weekly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Fixed Amount", "Random Amount"}));

        EditText etSingleMember = new EditText(MainActivity.this);
        etSingleMember.setHint("Member Name");
        llMembersContainer.addView(etSingleMember);

        spAmountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Fixed Amount")) {
                    etAmount.setVisibility(View.VISIBLE);
                    llAmountsContainer.setVisibility(View.GONE);
                } else {
                    etAmount.setVisibility(View.GONE);
                    llAmountsContainer.setVisibility(View.VISIBLE);
                    triggerDynamicAmountFields(etInstallmentsCount.getText().toString(), llAmountsContainer);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etInstallmentsCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (spAmountType.getSelectedItem().toString().equals("Random Amount")) {
                    triggerDynamicAmountFields(input, llAmountsContainer);
                }
            }
        });

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        etDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etChitName.getText().toString().trim();
                String freq = spFrequency.getSelectedItem().toString();
                String instStr = etInstallmentsCount.getText().toString().trim();
                String amtType = spAmountType.getSelectedItem().toString();
                String date = etDate.getText().toString().trim();

                if (name.isEmpty() || instStr.isEmpty() || date.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill in all basic fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int totalInst = Integer.parseInt(instStr);

                if (amtType.equals("Fixed Amount") && etAmount.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please specify an installment amount.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amtType.equals("Random Amount")) {
                    for (int i = 0; i < llAmountsContainer.getChildCount(); i++) {
                        if (((EditText) llAmountsContainer.getChildAt(i)).getText().toString().trim().isEmpty()) {
                            Toast.makeText(MainActivity.this, "Fill all dynamic amount rows.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                for (int i = 0; i < llMembersContainer.getChildCount(); i++) {
                    if (((EditText) llMembersContainer.getChildAt(i)).getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Fill in the member name field.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                long newChitId = dbHelper.insertChit(name, freq, totalInst, amtType, date);

                if (amtType.equals("Fixed Amount")) {
                    double fixAmt = Double.parseDouble(etAmount.getText().toString().trim());
                    for (int i = 1; i <= totalInst; i++) {
                        dbHelper.insertInstallmentAmount(newChitId, i, fixAmt);
                    }
                } else {
                    for (int i = 0; i < llAmountsContainer.getChildCount(); i++) {
                        double randAmt = Double.parseDouble(((EditText) llAmountsContainer.getChildAt(i)).getText().toString().trim());
                        dbHelper.insertInstallmentAmount(newChitId, (i + 1), randAmt);
                    }
                }

                for (int i = 0; i < llMembersContainer.getChildCount(); i++) {
                    dbHelper.insertMember(newChitId, ((EditText) llMembersContainer.getChildAt(i)).getText().toString().trim());
                }

                dialog.dismiss();
                // Pass the new ID to load and focus on it automatically
                populateChitSelector(newChitId);
            }
        });
    }

    private void triggerDynamicAmountFields(String countStr, LinearLayout container) {
        container.removeAllViews();
        if (!countStr.trim().isEmpty()) {
            int total = Integer.parseInt(countStr.trim());
            for (int i = 1; i <= total; i++) {
                EditText etAmtInput = new EditText(MainActivity.this);
                etAmtInput.setHint("Installment " + i + " Amount (₹)");
                etAmtInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                container.addView(etAmtInput);
            }
        }
    }
}
