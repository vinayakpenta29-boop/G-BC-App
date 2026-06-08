package com.example.chitfund;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private long chitId;
    private DatabaseHelper dbHelper;
    private Spinner spMembers, spInstallmentOptions;
    private TableLayout tlFundTable;
    private TextView tvFundTitle;
    private int totalInstallmentsCount;
    private String frequencyType;
    private String firstInstallmentDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        chitId = getIntent().getLongExtra("CHIT_ID", -1);
        dbHelper = new DatabaseHelper(this);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        final LinearLayout containerInstallment = findViewById(R.id.containerInstallment);
        final View containerFund = findViewById(R.id.containerFund);

        spMembers = findViewById(R.id.spMembers);
        spInstallmentOptions = findViewById(R.id.spInstallmentOptions);
        Button btnAddInstallment = findViewById(R.id.btnAddInstallment);
        tlFundTable = findViewById(R.id.tlFundTable);
        tvFundTitle = findViewById(R.id.tvFundTitle);

        tabLayout.addTab(tabLayout.newTab().setText("Installment"));
        tabLayout.addTab(tabLayout.newTab().setText("Fund"));

        loadChitMetaData();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    containerInstallment.setVisibility(View.VISIBLE);
                    containerFund.setVisibility(View.GONE);
                } else {
                    containerInstallment.setVisibility(View.GONE);
                    containerFund.setVisibility(View.VISIBLE);
                    refreshFundMatrixTable();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnAddInstallment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spMembers.getSelectedItem() == null || spInstallmentOptions.getSelectedItem() == null) return;
                
                String selectedMember = spMembers.getSelectedItem().toString();
                int instNum = spInstallmentOptions.getSelectedItemPosition() + 1;
                double currentTargetAmount = dbHelper.getInstallmentAmount(chitId, instNum);

                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                dbHelper.insertPayment(chitId, instNum, currentDate, selectedMember, currentTargetAmount);

                Toast.makeText(DetailActivity.this, "Installment Saved!", Toast.LENGTH_SHORT).show();
            }
        });
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

        // Calculate dynamic dates row keys array lists
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
                    cal.add(Calendar.DATE, i * 7); // Adds exactly 7 days per installment interval
                }
                calculatedDatesHeaders.add(sdfOutput.format(cal.getTime()));
            }
        } catch (Exception e) {
            calculatedDatesHeaders.add(firstInstallmentDateStr);
        }

        // Build the Header row dynamically
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

        // Build records rows for members entries list
        ArrayList<String> totalMembersList = dbHelper.getMembers(chitId);
        int serialCounter = 1;

        for (String name : totalMembersList) {
            TableRow memberRow = new TableRow(this);
            memberRow.setPadding(6, 10, 6, 10);

            TextView tvSerial = new TextView(this); tvSerial.setText(String.valueOf(serialCounter++)); tvSerial.setPadding(12, 6, 12, 6); memberRow.addView(tvSerial);
            TextView tvName = new TextView(this); tvName.setText(name); tvName.setPadding(12, 6, 12, 6); memberRow.addView(tvName);

            // Populate intersections status states blocks
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
}
