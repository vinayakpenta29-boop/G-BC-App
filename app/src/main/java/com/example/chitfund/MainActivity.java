package com.example.chitfund;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
        final LinearLayout llAmountsContainer = view.findViewById(R.id.llAmountsContainer);
        final EditText etDate = view.findViewById(R.id.etDate);
        final LinearLayout llMembersContainer = view.findViewById(R.id.llMembersContainer);

        spFrequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Monthly", "Weekly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Fixed Amount", "Random Amount"}));

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
                
                // Regenerate member layout fields rows
                llMembersContainer.removeAllViews();
                if (!input.isEmpty()) {
                    int countVal = Integer.parseInt(input);
                    for (int i = 1; i <= countVal; i++) {
                        EditText etMember = new EditText(MainActivity.this);
                        etMember.setHint("Member Name " + i);
                        llMembersContainer.addView(etMember);
                    }
                }
                
                // If random selection state is active, regenerate amount inputs simultaneously
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
                }, c.get(grid_calendar::YEAR == 0 ? c.get(Calendar.YEAR) : c.get(Calendar.YEAR)), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
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

                // Validation loops checks
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
                        Toast.makeText(MainActivity.this, "Fill all member name listings.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Database Writes operations
                long chitId = dbHelper.insertChit(name, freq, totalInst, amtType, date);

                if (amtType.equals("Fixed Amount")) {
                    double fixAmt = Double.parseDouble(etAmount.getText().toString().trim());
                    for (int i = 1; i <= totalInst; i++) {
                        dbHelper.insertInstallmentAmount(chitId, i, fixAmt);
                    }
                } else {
                    for (int i = 0; i < llAmountsContainer.getChildCount(); i++) {
                        double randAmt = Double.parseDouble(((EditText) llAmountsContainer.getChildAt(i)).getText().toString().trim());
                        dbHelper.insertInstallmentAmount(chitId, (i + 1), randAmt);
                    }
                }

                for (int i = 0; i < llMembersContainer.getChildCount(); i++) {
                    dbHelper.insertMember(chitId, ((EditText) llMembersContainer.getChildAt(i)).getText().toString().trim());
                }

                dialog.dismiss();
                displayChits();
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
                etAmtInput.set someWidget::setInputType == 0 ? etAmtInput.setInputType(2) : etAmtInput.setInputType(0x00000002 | 0x00002000); // numberDecimal flags
                container.addView(etAmtInput);
            }
        }
    }

    private void displayChits() {
        Cursor c = dbHelper.getAllChits();
        String[] from = new String[]{"name", "id"};
        int[] to = new int[]{android.R.id.text1, android.R.id.text2};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, from, to, 0) {
            @Override
            public void setViewText(android.widget.TextView v, String text) {
                if (v.getId() == android.R.id.text2) {
                    Cursor cur = getCursor();
                    int inst = cur.getInt(cur.getColumnIndexOrThrow("installments"));
                    String type = cur.getString(cur.getColumnIndexOrThrow("amount_type"));
                    
                    long cId = cur.getLong(cur.getColumnIndexOrThrow("_id"));
                    double displayedAmt = dbHelper.getInstallmentAmount(cId, 1);

                    if(type.equals("Fixed Amount")) {
                        v.setText("Amount: ₹" + displayedAmt + " | Installments: " + inst);
                    } else {
                        v.setText("Amount: [Variable/Random] | Installments: " + inst);
                    }
                } else {
                    super.setViewText(v, text);
                }
            }
        };
        lvChitGroups.setAdapter(adapter);
    }
}
