package com.example.chitfund;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long chitId = -1;

    private AutoCompleteTextView spChitSelector;
    private AutoCompleteTextView spMembers;
    private Button btnSelectInstallments;
    private TableLayout tlFundTable;
    private TextView tvFundTitle;
    private View llFormContainer;
    
    private TextView tvHistorySummary;
    private TableLayout tlHistoryTable;
    
    private int totalInstallmentsCount;
    private String frequencyType;
    private String firstInstallmentDateStr;

    private String[] installmentOptionsArray;
    private boolean[] checkedInstallments;
    private ArrayList<Integer> selectedInstallmentsList = new ArrayList<>();
    
    private ArrayList<DatabaseHelper.ChitItem> globalChitsList = new ArrayList<>();
    private ArrayList<String> globalMembersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle Bundle) {
        super.onCreate(Bundle);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);

        spChitSelector = findViewById(R.id.spChitSelector);
        spMembers = findViewById(R.id.spMembers);
        btnSelectInstallments = findViewById(R.id.btnSelectInstallments);
        Button btnAddInstallment = findViewById(R.id.btnAddInstallment);
        tlFundTable = findViewById(R.id.tlFundTable);
        tvFundTitle = findViewById(R.id.tvFundTitle);
        llFormContainer = findViewById(R.id.llFormContainer);
        
        tvHistorySummary = findViewById(R.id.tvHistorySummary);
        tlHistoryTable = findViewById(R.id.tlHistoryTable);

        spChitSelector.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatabaseHelper.ChitItem selected = globalChitsList.get(position);
                if (selected != null && selected.id != chitId) {
                    chitId = selected.id;
                    loadChitMetaData();
                    refreshFundMatrixTable();
                }
            }
        });

        btnSelectInstallments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMultiSelectInstallmentsDialog();
            }
        });

        populateChitSelector(-1);
        refreshTransactionHistory();

        btnAddInstallment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chitId == -1) {
                    Toast.makeText(MainActivity.this, "Please create a Chit Fund group first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                String selectedMember = spMembers.getText().toString().trim();
                if (selectedMember.isEmpty() || !globalMembersList.contains(selectedMember)) {
                    Toast.makeText(MainActivity.this, "Please select a valid member!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedInstallmentsList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please select at least one installment!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                for (int instNum : selectedInstallmentsList) {
                    if (!dbHelper.isPaymentMade(chitId, selectedMember, instNum)) {
                        double currentTargetAmount = dbHelper.getInstallmentAmount(chitId, instNum);
                        dbHelper.insertPayment(chitId, instNum, currentDate, selectedMember, currentTargetAmount);
                    }
                }

                Toast.makeText(MainActivity.this, "Installments Saved!", Toast.LENGTH_SHORT).show();
                
                resetInstallmentSelection();
                refreshFundMatrixTable();
                refreshTransactionHistory();
            }
        });
    }

    private void populateChitSelector(long targetChitId) {
        globalChitsList = dbHelper.getChitList();
        ArrayAdapter<DatabaseHelper.ChitItem> adapter = new ArrayAdapter<>(this, R.layout.list_item_premium, globalChitsList);
        spChitSelector.setAdapter(adapter);

        if (globalChitsList.isEmpty()) {
            chitId = -1;
            tvFundTitle.setText("No active Chit Fund found. Create one using the menu!");
            tlFundTable.removeAllViews();
            llFormContainer.setVisibility(View.GONE);
            return;
        }

        llFormContainer.setVisibility(View.VISIBLE);

        int selectIndex = 0;
        if (targetChitId != -1) {
            for (int i = 0; i < globalChitsList.size(); i++) {
                if (globalChitsList.get(i).id == targetChitId) {
                    selectIndex = i;
                    break;
                }
            }
        }
        
        DatabaseHelper.ChitItem targetItem = globalChitsList.get(selectIndex);
        spChitSelector.setText(targetItem.name, false);
        chitId = targetItem.id;
        loadChitMetaData();
        refreshFundMatrixTable();
    }

    private void loadChitMetaData() {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT name, frequency, installments, start_date FROM chits WHERE id = ?", new String[]{String.valueOf(chitId)});
        if (c.moveToFirst()) {
            String chitName = c.getString(0);
            frequencyType = c.getString(1);
            totalInstallmentsCount = c.getInt(2);
            firstInstallmentDateStr = c.getString(3);

            setTitle(chitName);
            tvFundTitle.setText("Chit Fund Matrix: " + chitName);

            globalMembersList = dbHelper.getMembers(chitId);
            ArrayAdapter<String> membersAdapter = new ArrayAdapter<>(this, R.layout.list_item_premium, globalMembersList);
            spMembers.setAdapter(membersAdapter);
            if(!globalMembersList.isEmpty()) {
                spMembers.setText(globalMembersList.get(0), false);
            } else {
                spMembers.setText("", false);
            }

            installmentOptionsArray = new String[totalInstallmentsCount];
            checkedInstallments = new boolean[totalInstallmentsCount];
            resetInstallmentSelection();

            for (int i = 1; i <= totalInstallmentsCount; i++) {
                double amt = dbHelper.getInstallmentAmount(chitId, i);
                installmentOptionsArray[i - 1] = "Installment " + i + " - ₹" + amt;
            }
        }
        c.close();
    }

    private void showMultiSelectInstallmentsDialog() {
        if (chitId == -1 || installmentOptionsArray == null) return;

        // Pass the explicit rounded theme overlay context reference
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this, R.style.PremiumRoundedAlertDialogTheme);
        builder.setTitle("Select Installments");
        
        builder.setMultiChoiceItems(installmentOptionsArray, checkedInstallments, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedInstallments[which] = isChecked;
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedInstallmentsList.clear();
                StringBuilder sb = new StringBuilder();
                
                for (int i = 0; i < checkedInstallments.length; i++) {
                    if (checkedInstallments[i]) {
                        selectedInstallmentsList.add(i + 1);
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(i + 1);
                    }
                }

                if (selectedInstallmentsList.isEmpty()) {
                    btnSelectInstallments.setText("Tap to Select Installments");
                } else {
                    btnSelectInstallments.setText("Selected Installments: " + sb.toString());
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        
        // Render and programmatically override the outer layout layer mask background bounds
        AlertDialog dialog = builder.create();
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
        }
    }

    private void resetInstallmentSelection() {
        if (checkedInstallments != null) {
            Arrays.fill(checkedInstallments, false);
        }
        selectedInstallmentsList.clear();
        btnSelectInstallments.setText("Tap to Select Installments");
    }

    private void refreshFundMatrixTable() {
        tlFundTable.removeAllViews();
        if (chitId == -1) return;

        ArrayList<String> calculatedDatesHeaders = new ArrayList<>();
        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfOutput = new SimpleDateFormat("d - MMM", Locale.getDefault());

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
        headerRow.setBackgroundResource(R.drawable.table_header_bg);
        headerRow.setPadding(6, 12, 6, 12);

        TextView hNo = new TextView(this); hNo.setText("No."); hNo.setPadding(20, 16, 20, 16); hNo.setTextSize(14); hNo.setTypeface(null, android.graphics.Typeface.BOLD); hNo.setTextColor(Color.WHITE); hNo.setGravity(Gravity.CENTER); headerRow.addView(hNo);
        TextView hName = new TextView(this); hName.setText("Member Name"); hName.setPadding(20, 16, 20, 16); hName.setTextSize(14); hName.setTypeface(null, android.graphics.Typeface.BOLD); hName.setTextColor(Color.WHITE); hName.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); headerRow.addView(hName);

        for (String dateStr : calculatedDatesHeaders) {
            TextView hDate = new TextView(this);
            hDate.setText(dateStr);
            hDate.setPadding(20, 16, 20, 16);
            hDate.setTextSize(14);
            hDate.setTypeface(null, android.graphics.Typeface.BOLD);
            hDate.setTextColor(Color.WHITE);
            hDate.setGravity(Gravity.CENTER);
            headerRow.addView(hDate);
        }
        tlFundTable.addView(headerRow);

        int serialCounter = 1;
        for (String name : globalMembersList) {
            TableRow memberRow = new TableRow(this);
            memberRow.setPadding(6, 8, 6, 8);

            TextView tvSerial = new TextView(this); tvSerial.setText(String.valueOf(serialCounter++)); tvSerial.setPadding(20, 16, 20, 16); tvSerial.setTextColor(Color.parseColor("#78909C")); tvSerial.setGravity(Gravity.CENTER); memberRow.addView(tvSerial);
            TextView tvName = new TextView(this); tvName.setText(name); tvName.setPadding(20, 16, 20, 16); tvName.setTypeface(null, android.graphics.Typeface.BOLD); tvName.setTextColor(Color.parseColor("#263238")); tvName.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); memberRow.addView(tvName);

            for (int i = 1; i <= totalInstallmentsCount; i++) {
                LinearLayout cellContainer = new LinearLayout(this);
                cellContainer.setPadding(12, 8, 12, 8);
                cellContainer.setGravity(Gravity.CENTER);

                TextView tvStatusCell = new TextView(this);
                tvStatusCell.setTextSize(13);
                tvStatusCell.setGravity(Gravity.CENTER);
                tvStatusCell.setPadding(16, 6, 16, 6);
                tvStatusCell.setTypeface(null, android.graphics.Typeface.BOLD);
                
                if (dbHelper.isPaymentMade(chitId, name, i)) {
                    tvStatusCell.setText(" Paid ✅ ");
                    tvStatusCell.setTextColor(Color.parseColor("#2E7D32"));
                    tvStatusCell.setBackgroundResource(R.drawable.badge_paid_bg);
                } else {
                    tvStatusCell.setText(" Pending ");
                    tvStatusCell.setTextColor(Color.parseColor("#78909C"));
                    tvStatusCell.setBackgroundResource(R.drawable.badge_unpaid_bg);
                }
                
                cellContainer.addView(tvStatusCell);
                memberRow.addView(cellContainer);
            }
            tlFundTable.addView(memberRow);
        }
    }

    private void refreshTransactionHistory() {
        tlHistoryTable.removeAllViews();
        
        Cursor cursor = dbHelper.getTransactionHistoryCursor();
        double runningCashTotal = 0;
        int transactionEntriesCount = 0;

        TableRow headRow = new TableRow(this);
        headRow.setBackgroundResource(R.drawable.table_header_bg);
        headRow.setPadding(6, 12, 6, 12);

        String[] headers = {"Date", "Chit Group", "Member Name", "Inst.", "Amount Paid"};
        for (String headerText : headers) {
            TextView tvHead = new TextView(this);
            tvHead.setText(headerText);
            tvHead.setPadding(20, 16, 20, 16);
            tvHead.setTextSize(14);
            tvHead.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHead.setTextColor(Color.WHITE);
            tvHead.setGravity(Gravity.CENTER);
            headRow.addView(tvHead);
        }
        tlHistoryTable.addView(headRow);

        while (cursor.moveToNext()) {
            String entryDate = cursor.getString(0);
            String chitGroupName = cursor.getString(1);
            String memberName = cursor.getString(2);
            int installmentNum = cursor.getInt(3);
            double amountPaid = cursor.getDouble(4);

            runningCashTotal += amountPaid;
            transactionEntriesCount++;

            TableRow tr = new TableRow(this);
            tr.setPadding(6, 8, 6, 8);

            TextView tvDate = new TextView(this); tvDate.setText(entryDate); tvDate.setPadding(20, 16, 20, 16); tvDate.setTextColor(Color.parseColor("#546E7A")); tvDate.setGravity(Gravity.CENTER); tr.addView(tvDate);
            TextView tvChit = new TextView(this); tvChit.setText(chitGroupName); tvChit.setPadding(20, 16, 20, 16); tvChit.setTextColor(Color.parseColor("#263238")); tvChit.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); tr.addView(tvChit);
            TextView tvMem = new TextView(this); tvMem.setText(memberName); tvMem.setPadding(20, 16, 20, 16); tvMem.setTextColor(Color.parseColor("#263238")); tvMem.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); tr.addView(tvMem);
            
            LinearLayout badgeWrapper = new LinearLayout(this);
            badgeWrapper.setPadding(10, 6, 10, 6);
            badgeWrapper.setGravity(Gravity.CENTER);
            TextView tvInst = new TextView(this); 
            tvInst.setText("Installment " + installmentNum); 
            tvInst.setPadding(14, 4, 14, 4); 
            tvInst.setTextSize(12);
            tvInst.setTextColor(Color.parseColor("#455A64"));
            tvInst.setBackgroundResource(R.drawable.badge_unpaid_bg);
            badgeWrapper.addView(tvInst);
            tr.addView(badgeWrapper);
            
            TextView tvAmt = new TextView(this); 
            tvAmt.setText("₹" + amountPaid); 
            tvAmt.setPadding(20, 16, 20, 16); 
            tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAmt.setTextColor(Color.parseColor("#2E7D32"));
            tvAmt.setGravity(Gravity.CENTER);
            tr.addView(tvAmt);

            tlHistoryTable.addView(tr);
        }
        cursor.close();

        tvHistorySummary.setText("Total Funds Collected: ₹" + runningCashTotal + "  |  Total Transactions: " + transactionEntriesCount);
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.PremiumRoundedAlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_new_chit, null);

        final TextInputEditText etChitName = view.findViewById(R.id.etChitName);
        final AutoCompleteTextView spFrequency = view.findViewById(R.id.spFrequency);
        final TextInputEditText etInstallmentsCount = view.findViewById(R.id.etInstallmentsCount);
        final AutoCompleteTextView spAmountType = view.findViewById(R.id.spAmountType);
        final TextInputEditText etAmount = view.findViewById(R.id.etAmount);
        final View tlAmountWrapper = view.findViewById(R.id.tlAmountWrapper);
        final LinearLayout llAmountsContainer = view.findViewById(R.id.llAmountsContainer);
        final TextInputEditText etDate = view.findViewById(R.id.etDate);
        final LinearLayout llMembersContainer = view.findViewById(R.id.llMembersContainer);

        final ArrayList<TextInputEditText> dynamicAmountFields = new ArrayList<>();
        final ArrayList<TextInputEditText> dynamicMemberFields = new ArrayList<>();

        spFrequency.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, new String[]{"Monthly", "Weekly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, new String[]{"Fixed Amount", "Random Amount"}));

        TextInputLayout tlMemberWrap = new TextInputLayout(MainActivity.this);
        tlMemberWrap.setHint("Primary Member Name");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        tlMemberWrap.setLayoutParams(lp);

        TextInputEditText etSingleMember = new TextInputEditText(MainActivity.this);
        tlMemberWrap.addView(etSingleMember);
        llMembersContainer.addView(tlMemberWrap);
        dynamicMemberFields.add(etSingleMember);

        spAmountType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Fixed Amount")) {
                    tlAmountWrapper.setVisibility(View.VISIBLE);
                    llAmountsContainer.setVisibility(View.GONE);
                } else {
                    tlAmountWrapper.setVisibility(View.GONE);
                    llAmountsContainer.setVisibility(View.VISIBLE);
                    triggerDynamicAmountFields(etInstallmentsCount.getText().toString(), llAmountsContainer, dynamicAmountFields);
                }
            }
        });

        etInstallmentsCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (spAmountType.getText().toString().equals("Random Amount")) {
                    triggerDynamicAmountFields(input, llAmountsContainer, dynamicAmountFields);
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
        builder.setPositiveButton("Create Group", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etChitName.getText().toString().trim();
                String freq = spFrequency.getText().toString();
                String instStr = etInstallmentsCount.getText().toString().trim();
                String amtType = spAmountType.getText().toString();
                String date = etDate.getText().toString().trim();

                if (name.isEmpty() || instStr.isEmpty() || date.isEmpty() || freq.isEmpty() || amtType.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill in all basic fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int totalInst = Integer.parseInt(instStr);

                if (amtType.equals("Fixed Amount") && etAmount.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please specify an installment amount.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amtType.equals("Random Amount")) {
                    if(dynamicAmountFields.size() < totalInst) {
                        Toast.makeText(MainActivity.this, "Generate tracking amounts completely first.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (TextInputEditText field : dynamicAmountFields) {
                        if (field.getText().toString().trim().isEmpty()) {
                            Toast.makeText(MainActivity.this, "Fill all dynamic amount fields.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                for (TextInputEditText field : dynamicMemberFields) {
                    if (field.getText().toString().trim().isEmpty()) {
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
                    for (int i = 0; i < dynamicAmountFields.size(); i++) {
                        double randAmt = Double.parseDouble(dynamicAmountFields.get(i).getText().toString().trim());
                        dbHelper.insertInstallmentAmount(newChitId, (i + 1), randAmt);
                    }
                }

                for (TextInputEditText field : dynamicMemberFields) {
                    dbHelper.insertMember(newChitId, field.getText().toString().trim());
                }

                dialog.dismiss();
                populateChitSelector(newChitId);
                refreshTransactionHistory();
            }
        });
    }

    private void triggerDynamicAmountFields(String countStr, LinearLayout container, ArrayList<TextInputEditText> fieldTrackerList) {
        container.removeAllViews();
        fieldTrackerList.clear();
        if (!countStr.trim().isEmpty()) {
            int total = Integer.parseInt(countStr.trim());
            for (int i = 1; i <= total; i++) {
                TextInputLayout wrap = new TextInputLayout(MainActivity.this);
                wrap.setHint("Installment " + i + " Amount (₹)");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                wrap.setLayoutParams(lp);

                TextInputEditText etAmtInput = new TextInputEditText(MainActivity.this);
                etAmtInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                
                wrap.addView(etAmtInput);
                container.addView(wrap);
                fieldTrackerList.add(etAmtInput);
            }
        }
    }
}
