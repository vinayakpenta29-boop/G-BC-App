package com.example.chitfund;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvChitGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        lvChitGroups = findViewById(R.id.lvChitGroups);

        displayChits();

        lvChitGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("CHIT_ID", id);
                startActivity(intent);
            }
        });
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
        final EditText etDate = view.findViewById(R.id.etDate);
        final LinearLayout llMembersContainer = view.findViewById(R.id.llMembersContainer);

        // Configure Spinners
        spFrequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Monthly", "Weekly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Fixed Amount", "Random Amount"}));

        // Date Picker Setup
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

        // Watch installment input count to dynamically inflate member input boxes
        etInstallmentsCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                llMembersContainer.removeAllViews();
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    int countVal = Integer.parseInt(input);
                    for (int i = 1; i <= countVal; i++) {
                        EditText etMember = new EditText(MainActivity.this);
                        etMember.setHint("Member Name " + i);
                        llMembersContainer.addView(etMember);
                    }
                }
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
                String amtStr = etAmount.getText().toString().trim();
                String date = etDate.getText().toString().trim();

                if (name.isEmpty() || instStr.isEmpty() || amtStr.isEmpty() || date.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill in all parameters.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int totalInst = Integer.parseInt(instStr);
                double baseAmt = Double.parseDouble(amtStr);

                // Validation to check if all dynamic members names fields have text
                for (int i = 0; i < llMembersContainer.getChildCount(); i++) {
                    EditText field = (EditText) llMembersContainer.getChildAt(i);
                    if (field.getText().toString().trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Fill all member name lines.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                long chitId = dbHelper.insertChit(name, freq, totalInst, amtType, baseAmt, date);
                for (int i = 0; i < llMembersContainer.getChildCount(); i++) {
                    EditText field = (EditText) llMembersContainer.getChildAt(i);
                    dbHelper.insertMember(chitId, field.getText().toString().trim());
                }

                dialog.dismiss();
                displayChits();
            }
        });
    }

    private void displayChits() {
        Cursor c = dbHelper.getAllChits();
        String[] from = new String[]{"name", "amount", "installments"};
        int[] to = new int[]{android.R.id.text1, android.R.id.text2, android.R.id.text2};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, from, to, 0) {
            @Override
            public void setViewText(android.widget.TextView v, String text) {
                if (v.getId() == android.R.id.text2) {
                    // Pull explicit variables manually to properly construct layout description items string
                    Cursor cur = getCursor();
                    double amt = cur.getDouble(cur.getColumnIndexOrThrow("amount"));
                    int inst = cur.getInt(cur.getColumnIndexOrThrow("installments"));
                    v.setText("Amount: ₹" + amt + " | Installments: " + inst);
                } else {
                    super.setViewText(v, text);
                }
            }
        };
        lvChitGroups.setAdapter(adapter);
    }
}
