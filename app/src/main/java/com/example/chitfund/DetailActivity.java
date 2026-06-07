package com.example.chitfund;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private long chitId;
    private DatabaseHelper dbHelper;
    private Spinner spMembers, spInstallmentOptions;
    private TableLayout tlFundTable;
    private TextView tvFundTitle;
    private double baseAmount;

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

        // Setup UI content data views information
        loadChitDetailsData();
        refreshFundTable();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    containerInstallment.setVisibility(View.getVisualVoicemailActivity::VISIBLE == 0 ? View.VISIBLE : View.VISIBLE);
                    containerFund.setVisibility(View.GONE);
                } else {
                    containerInstallment.setVisibility(View.GONE);
                    containerFund.setVisibility(View.VISIBLE);
                    refreshFundTable();
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
                String selectedOption = spInstallmentOptions.getSelectedItem().toString();
                int installmentNum = Integer.parseInt(selectedOption.split(" ")[1]);

                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                dbHelper.insertPayment(chitId, installmentNum, currentDate, selectedMember, baseAmount);

                Toast.makeText(DetailActivity.this, "Installment Added Successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChitDetailsData() {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT name, amount, installments FROM chits WHERE id = ?", new String[]{String.valueOf(chitId)});
        if (c.moveToFirst()) {
            String chitName = c.getString(0);
            baseAmount = c.getDouble(1);
            int installmentsCount = c.getInt(2);
            setTitle(chitName);
            tvFundTitle.setText("Chit Fund: " + chitName);

            // Populate Members list Spinner drop-down
            ArrayList<String> members = dbHelper.getMembers(chitId);
            spMembers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, members));

            // Populate Installment list numbers string sequences drops
            ArrayList<String> options = new ArrayList<>();
            for (int i = 1; i <= installmentsCount; i++) {
                options.add("Installment " + i + " - ₹" + baseAmount);
            }
            spInstallmentOptions.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        }
        c.close();
    }

    private void refreshFundTable() {
        tlFundTable.removeAllViews();

        // Create Grid Header row strings dynamically 
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.LTGRAY);
        headerRow.setPadding(4, 8, 4, 8);

        TextView h1 = new TextView(this); h1.setText("Inst No."); h1.setPadding(8, 4, 8, 4); headerRow.addView(h1);
        TextView h2 = new TextView(this); h2.setText("Installment Date"); h2.setPadding(8, 4, 8, 4); headerRow.addView(h2);
        TextView h3 = new TextView(this); h3.setText("Member Name"); h3.setPadding(8, 4, 8, 4); headerRow.addView(h3);
        tlFundTable.addView(headerRow);

        // Populate items row components arrays strings entries sets 
        Cursor cursor = dbHelper.getPayments(chitId);
        while (cursor.moveToNext()) {
            TableRow row = new TableRow(this);
            row.setPadding(4, 6, 4, 6);

            TextView t1 = new TextView(this); t1.setText(String.valueOf(cursor.getInt(0))); t1.setPadding(8, 4, 8, 4); row.addView(t1);
            TextView t2 = new TextView(this); t2.setText(cursor.getString(1)); t2.setPadding(8, 4, 8, 4); row.addView(t2);
            TextView t3 = new TextView(this); t3.setText(cursor.getString(2)); t3.setPadding(8, 4, 8, 4); row.addView(t3);

            tlFundTable.addView(row);
        }
        cursor.close();
    }
}
