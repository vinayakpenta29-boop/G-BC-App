package com.example.chitfund;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etGroupName, etTotalValue, etMonths;
    private Button btnSaveGroup;
    private TextView tvDisplayLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind interactive views
        etGroupName = findViewById(R.id.etGroupName);
        etTotalValue = findViewById(R.id.etTotalValue);
        etMonths = findViewById(R.id.etMonths);
        btnSaveGroup = findViewById(R.id.btnSaveGroup);
        tvDisplayLog = findViewById(R.id.tvDisplayLog);

        // Load existing history from database on initialization
        displaySavedGroups();

        // Assign interaction events
        btnSaveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChitGroup();
            }
        });
    }

    private void saveChitGroup() {
        String name = etGroupName.getText().toString().trim();
        String valueStr = etTotalValue.getText().toString().trim();
        String monthsStr = etMonths.getText().toString().trim();

        // Ensure no empty fields are evaluated
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(valueStr) || TextUtils.isEmpty(monthsStr)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalValue = Double.parseDouble(valueStr);
        int months = Integer.parseInt(monthsStr);

        if (months <= 0 || totalValue <= 0) {
            Toast.makeText(this, "Enter values greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Write directly to local SQLite engine
        long id = dbHelper.createGroup(name, totalValue, months);

        if (id > 0) {
            Toast.makeText(this, "Group Created Successfully!", Toast.LENGTH_SHORT).show();
            
            // Clean view space for next entry
            etGroupName.setText("");
            etTotalValue.setText("");
            etMonths.setText("");
            
            // Re-render logged entries area
            displaySavedGroups();
        } else {
            Toast.makeText(this, "Failed to save group", Toast.LENGTH_SHORT).show();
        }
    }

    private void displaySavedGroups() {
        Cursor cursor = dbHelper.getAllGroups();
        if (cursor == null || cursor.getCount() == 0) {
            tvDisplayLog.setText("No groups added yet.");
            if (cursor != null) cursor.close();
            return;
        }

        StringBuilder builder = new StringBuilder();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GROUP_NAME));
            double total = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TOTAL_VALUE));
            int months = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MONTHS));
            double contribution = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MIN_CONTRIBUTION));

            builder.append("ID: ").append(id)
                   .append(" | Name: ").append(name).append("\n")
                   .append("Total Pool: ₹").append(total).append("\n")
                   .append("Duration: ").append(months).append(" Months / Members\n")
                   .append("Base Premium: ₹").append(contribution).append("/mo\n")
                   .append("----------------------------------------\n\n");
        }
        cursor.close();
        tvDisplayLog.setText(builder.toString());
    }
}
