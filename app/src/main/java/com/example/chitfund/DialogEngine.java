package com.example.chitfund;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DialogEngine {
    private MainActivity activity;

    public DialogEngine(MainActivity activity) {
        this.activity = activity;
    }

    public void showConfirmPaymentDialog() {
        if (activity.chitId == null) {
            Toast.makeText(activity, "Please create a Chit Fund group first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String TylerMember = activity.spMembers.getText().toString().trim();
        if (TylerMember.isEmpty() || !activity.globalMembersList.contains(TylerMember)) {
            Toast.makeText(activity, "Please select a valid member!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (activity.selectedInstallmentsList.isEmpty()) {
            Toast.makeText(activity, "Please select at least one installment!", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout wrapperLayout = new LinearLayout(activity);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.setPadding(60, 40, 60, 0);

        double totalDue = 0.0;
        for (int instNum : activity.selectedInstallmentsList) {
            double expectedAmt = activity.getSpecificCachedMemberInstallmentAmount(activity.chitId, TylerMember, instNum);
            double paidAmt = activity.globalPaymentsCache.containsKey(activity.chitId + "_" + TylerMember + "_" + instNum) ? activity.globalPaymentsCache.get(activity.chitId + "_" + TylerMember + "_" + instNum) : 0.0;
            totalDue += (expectedAmt - paidAmt);
        }

        TextView tvMsg = new TextView(activity);
        tvMsg.setText("Recording payments for " + activity.selectedInstallmentsList.size() + " installment(s). Total Remaining Due: ₹" + String.format(Locale.getDefault(), "%,.1f", totalDue));
        tvMsg.setTextColor(Color.parseColor("#475569"));
        tvMsg.setTextSize(14);
        tvMsg.setPadding(0, 0, 0, 40);
        wrapperLayout.addView(tvMsg);

        TextInputLayout tlPay = new TextInputLayout(activity);
        tlPay.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlPay.setBoxCornerRadii(16f, 16f, 16f, 16f);
        tlPay.setBoxBackgroundColor(Color.TRANSPARENT);
        tlPay.setHint("Amount Being Paid (₹)");
        LinearLayout.LayoutParams payLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        payLp.setMargins(0, 0, 0, 30);
        tlPay.setLayoutParams(payLp);

        TextInputEditText etPay = new TextInputEditText(tlPay.getContext());
        etPay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPay.setText(String.format(Locale.getDefault(), "%.0f", totalDue));
        tlPay.addView(etPay);
        wrapperLayout.addView(tlPay);

        TextInputLayout tlNote = new TextInputLayout(activity);
        tlNote.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlNote.setBoxCornerRadii(16f, 16f, 16f, 16f);
        tlNote.setBoxBackgroundColor(Color.TRANSPARENT);
        tlNote.setHint("Notes (Optional)");

        TextInputEditText etNote = new TextInputEditText(tlNote.getContext());
        etNote.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tlNote.addView(etNote);
        wrapperLayout.addView(tlNote);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Confirm Payment");
        builder.setView(wrapperLayout);
        builder.setPositiveButton("Confirm & Save", (dialog, which) -> {

            String noteText = etNote.getText().toString().trim();
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String payStr = etPay.getText().toString().trim();
            if (payStr.isEmpty()) payStr = "0";

            double amountToDistribute = Double.parseDouble(payStr);

            for (int instNum : activity.selectedInstallmentsList) {
                if (amountToDistribute <= 0) break;

                double expectedAmt = activity.getSpecificCachedMemberInstallmentAmount(activity.chitId, TylerMember, instNum);
                double alreadyPaid = activity.globalPaymentsCache.containsKey(activity.chitId + "_" + TylerMember + "_" + instNum) ? activity.globalPaymentsCache.get(activity.chitId + "_" + TylerMember + "_" + instNum) : 0.0;
                double remainingForThisStep = expectedAmt - alreadyPaid;

                if (remainingForThisStep > 0) {
                    double paymentForThisStep = Math.min(remainingForThisStep, amountToDistribute);
                    amountToDistribute -= paymentForThisStep;

                    Map<String, Object> paymentPayload = new HashMap<>();
                    paymentPayload.put("chitId", activity.chitId);
                    paymentPayload.put("installment_num", instNum);
                    paymentPayload.put("member_name", TylerMember);
                    paymentPayload.put("amount", paymentForThisStep);
                    paymentPayload.put("date", currentDate);
                    paymentPayload.put("timestamp", System.currentTimeMillis());
                    paymentPayload.put("notes", noteText);

                    activity.firestore.collection("payments").add(paymentPayload);
                }
            }

            Toast.makeText(activity, "Payments Saved & Distributed Successfully!", Toast.LENGTH_SHORT).show();
            activity.resetInstallmentSelection();
            activity.refreshFundMatrixTable();
            activity.refreshTransactionHistory();
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog confDialog = builder.create();
        confDialog.show();
        if (confDialog.getWindow() != null) confDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
    }

    public void showMultiSelectInstallmentsDialog() {
        if (activity.chitId == null) return;
        final String member = activity.spMembers.getText().toString().trim();
        if (member.isEmpty()) return;

        final ArrayList<Integer> openInstallmentNumbers = new ArrayList<>();
        ArrayList<CharSequence> filteredOptionsList = new ArrayList<>();

        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfDialogOutput = new SimpleDateFormat("d MMM yy", Locale.getDefault());
        Calendar todayCal = Calendar.getInstance();

        for (int i = 1; i <= activity.totalInstallmentsCount; i++) {
            
            double expectedAmt = activity.getSpecificCachedMemberInstallmentAmount(activity.chitId, member, i);
            String compositeKey = activity.chitId + "_" + member + "_" + i;
            double paidAmt = activity.globalPaymentsCache.containsKey(compositeKey) ? activity.globalPaymentsCache.get(compositeKey) : 0.0;
            double remainingAmt = expectedAmt - paidAmt;

            if (remainingAmt > 0) {
                openInstallmentNumbers.add(i);

                String dateLabel = "";
                boolean isCurrent = false;
                
                try {
                    Date startDate = sdfInput.parse(activity.firstInstallmentDateStr);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startDate);
                    if ("Monthly".equals(activity.frequencyType)) {
                        cal.add(Calendar.MONTH, i - 1);
                    } else if ("Half Yearly".equals(activity.frequencyType)) {
                        cal.add(Calendar.MONTH, (i - 1) * 6);
                    } else {
                        cal.add(Calendar.DATE, (i - 1) * 7);
                    }
                    
                    int cY = cal.get(Calendar.YEAR);
                    int tY = todayCal.get(Calendar.YEAR);
                    int cM = cal.get(Calendar.MONTH);
                    int tM = todayCal.get(Calendar.MONTH);
                    
                    if ("Weekly".equals(activity.frequencyType)) {
                        int cW = cal.get(Calendar.WEEK_OF_YEAR);
                        int tW = todayCal.get(Calendar.WEEK_OF_YEAR);
                        if (cW == tW && cY == tY) { isCurrent = true; }
                    } else {
                        if (cY == tY && cM == tM) { isCurrent = true; }
                    }
                    
                    dateLabel = "( " + sdfDialogOutput.format(cal.getTime()) + ") ";
                } catch (Exception ignored) {}

                String rawRowStr = dateLabel + "Inst. " + i + " - ₹" + String.format(Locale.getDefault(), "%,.1f", remainingAmt) + " Due";
                
                android.text.SpannableString spRow = new android.text.SpannableString(rawRowStr);
                if (isCurrent) {
                    spRow.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#15803D")), 0, rawRowStr.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spRow.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, rawRowStr.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                filteredOptionsList.add(spRow);
            }
        }

        if (openInstallmentNumbers.isEmpty()) {
            Toast.makeText(activity, "All installments are already fully paid!", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] optionsArray = filteredOptionsList.toArray(new CharSequence[0]);
        final boolean[] localCheckedTracker = new boolean[optionsArray.length];

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Select Pending Installments");
        builder.setMultiChoiceItems(optionsArray, localCheckedTracker, (dialog, which, isChecked) -> localCheckedTracker[which] = isChecked);
        builder.setPositiveButton("OK", (dialog, which) -> {
            activity.selectedInstallmentsList.clear();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < localCheckedTracker.length; i++) {
                if (localCheckedTracker[i]) {
                    int realNum = openInstallmentNumbers.get(i);
                    activity.selectedInstallmentsList.add(realNum);
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(realNum);
                }
            }
            activity.btnSelectInstallments.setText(activity.selectedInstallmentsList.isEmpty() ? "Tap to Select Installments" : "Selected Inst: " + sb.toString());
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
    }

    public void showLogAdvanceDialog() {
        if (activity.chitId == null) {
            Toast.makeText(activity, "Please create/select a Chit Group first.", Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_log_advance, null);

        final AutoCompleteTextView acMem = view.findViewById(R.id.acMem);
        final TextInputEditText etInst = view.findViewById(R.id.etInstNum);
        final TextInputEditText etAdvanceAmt = view.findViewById(R.id.etAdvanceAmt);
        final TextInputEditText etAmt = view.findViewById(R.id.etNewAmt);
        
        LinearLayout wrapperLayout = new LinearLayout(activity);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.addView(view);
        
        TextInputLayout tlNote = new TextInputLayout(activity);
        tlNote.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlNote.setBoxCornerRadii(16f, 16f, 16f, 16f); 
        tlNote.setBoxBackgroundColor(Color.TRANSPARENT); 
        tlNote.setHint("Notes (Optional)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(60, 0, 60, 40); 
        tlNote.setLayoutParams(lp);
        
        TextInputEditText etNote = new TextInputEditText(tlNote.getContext());
        etNote.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tlNote.addView(etNote);
        wrapperLayout.addView(tlNote);

        acMem.setAdapter(new ArrayAdapter<>(activity, R.layout.list_item_member, activity.globalMembersList));
        builder.setView(wrapperLayout);
        builder.setPositiveButton("Save Advance Rules", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create(); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String memName = acMem.getText().toString().trim();
            String instStr = etInst.getText().toString().trim();
            String advAmtStr = etAdvanceAmt.getText().toString().trim();
            String amtStr = etAmt.getText().toString().trim();
            String noteText = etNote.getText().toString().trim();

            if (memName.isEmpty() || instStr.isEmpty() || advAmtStr.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(activity, "Please fill out all fields completely.", Toast.LENGTH_SHORT).show();
                return;
            }

            int instNum = Integer.parseInt(instStr);
            double advAmt = Double.parseDouble(advAmtStr);
            double newAmt = Double.parseDouble(amtStr);

            if (instNum < 1 || instNum > activity.totalInstallmentsCount) {
                Toast.makeText(activity, "Invalid installment milestone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Map<String, Object> advancePayload = new HashMap<>();
            advancePayload.put("chitId", activity.chitId); advancePayload.put("installment_num", instNum);
            advancePayload.put("member_name", memName); advancePayload.put("advance_amount", advAmt);
            advancePayload.put("new_amount", newAmt); advancePayload.put("date", currentDate);
            advancePayload.put("notes", noteText);

            activity.firestore.collection("advances").add(advancePayload).addOnSuccessListener(ref -> {
                Toast.makeText(activity, "Advance configuration saved to Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
    }

    public void showEditAdvanceDialog(QueryDocumentSnapshot doc) {
        String docId = doc.getId();
        String currentChitId = doc.getString("chitId");
        String currentMember = doc.getString("member_name");
        long currentInst = doc.getLong("installment_num") != null ? doc.getLong("installment_num") : 0;
        double currentAdvAmt = doc.getDouble("advance_amount") != null ? doc.getDouble("advance_amount") : 0.0;
        double currentNewAmt = doc.getDouble("new_amount") != null ? doc.getDouble("new_amount") : 0.0;
        String currentNotes = doc.getString("notes") != null ? doc.getString("notes") : "";

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_log_advance, null);

        final AutoCompleteTextView acMem = view.findViewById(R.id.acMem);
        final TextInputEditText etInst = view.findViewById(R.id.etInstNum);
        final TextInputEditText etAdvanceAmt = view.findViewById(R.id.etAdvanceAmt);
        final TextInputEditText etAmt = view.findViewById(R.id.etNewAmt);
        
        acMem.setText(currentMember, false);
        etInst.setText(String.valueOf(currentInst));
        etAdvanceAmt.setText(String.format(Locale.getDefault(), "%.1f", currentAdvAmt).replace(".0", ""));
        etAmt.setText(String.format(Locale.getDefault(), "%.1f", currentNewAmt).replace(".0", ""));

        LinearLayout wrapperLayout = new LinearLayout(activity);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.addView(view);
        
        TextInputLayout tlNote = new TextInputLayout(activity);
        tlNote.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlNote.setBoxCornerRadii(16f, 16f, 16f, 16f); 
        tlNote.setBoxBackgroundColor(Color.TRANSPARENT); 
        tlNote.setHint("Notes (Optional)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(60, 0, 60, 40); 
        tlNote.setLayoutParams(lp);
        
        TextInputEditText etNote = new TextInputEditText(tlNote.getContext());
        etNote.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        etNote.setText(currentNotes); 
        tlNote.addView(etNote);
        wrapperLayout.addView(tlNote);

        ArrayList<String> availableMembers = activity.globalChitMembersCache.containsKey(currentChitId) ? activity.globalChitMembersCache.get(currentChitId) : new ArrayList<>();
        acMem.setAdapter(new ArrayAdapter<>(activity, R.layout.list_item_member, availableMembers));

        builder.setView(wrapperLayout);
        builder.setTitle("Edit Advance Record");
        builder.setPositiveButton("Save Updates", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create(); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String memName = acMem.getText().toString().trim();
            String instStr = etInst.getText().toString().trim();
            String advAmtStr = etAdvanceAmt.getText().toString().trim();
            String amtStr = etAmt.getText().toString().trim();
            String noteText = etNote.getText().toString().trim();

            if (memName.isEmpty() || instStr.isEmpty() || advAmtStr.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(activity, "Please fill out all fields completely.", Toast.LENGTH_SHORT).show();
                return;
            }

            int instNum = Integer.parseInt(instStr);
            double advAmt = Double.parseDouble(advAmtStr);
            double newAmt = Double.parseDouble(amtStr);

            int maxInstForChit = activity.globalChitInstallmentsCountCache.containsKey(currentChitId) ? activity.globalChitInstallmentsCountCache.get(currentChitId) : 999;

            if (instNum < 1 || instNum > maxInstForChit) {
                Toast.makeText(activity, "Invalid installment milestone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("installment_num", instNum);
            updatePayload.put("member_name", memName);
            updatePayload.put("advance_amount", advAmt);
            updatePayload.put("new_amount", newAmt);
            updatePayload.put("notes", noteText);
            
            activity.firestore.collection("advances").document(docId).update(updatePayload).addOnSuccessListener(ref -> {
                Toast.makeText(activity, "Advance record updated successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(activity, "Error updating record.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    public void showNewChitDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_new_chit, null);

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

        spFrequency.setAdapter(new ArrayAdapter<>(activity, R.layout.list_item_premium, new String[]{"Monthly", "Weekly", "Half Yearly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(activity, R.layout.list_item_premium, new String[]{"Fixed Amount", "Random Amount"}));

        TextInputLayout tlMemberWrap = new TextInputLayout(activity);
        tlMemberWrap.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlMemberWrap.setBoxCornerRadii(16f, 16f, 16f, 16f); 
        tlMemberWrap.setBoxBackgroundColor(Color.TRANSPARENT); 
        tlMemberWrap.setHint("Primary Member Name");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24); 
        tlMemberWrap.setLayoutParams(lp);

        TextInputEditText etSingleMember = new TextInputEditText(tlMemberWrap.getContext()); 
        etSingleMember.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tlMemberWrap.addView(etSingleMember);
        llMembersContainer.addView(tlMemberWrap); dynamicMemberFields.add(etSingleMember);

        spAmountType.setOnItemClickListener((parent, v, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            if (selected.equals("Fixed Amount")) {
                tlAmountWrapper.setVisibility(View.VISIBLE); llAmountsContainer.setVisibility(View.GONE);
            } else {
                tlAmountWrapper.setVisibility(View.GONE); llAmountsContainer.setVisibility(View.VISIBLE);
                triggerDynamicAmountFields(etInstallmentsCount.getText().toString(), llAmountsContainer, dynamicAmountFields);
            }
        });

        etInstallmentsCount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (spAmountType.getText().toString().equals("Random Amount")) {
                    triggerDynamicAmountFields(s.toString(), llAmountsContainer, dynamicAmountFields);
                }
            }
        });

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(activity, (view1, year, month, dayOfMonth) -> etDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(view); builder.setPositiveButton("Create Group", null); builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.create(); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etChitName.getText().toString().trim();
            String freq = spFrequency.getText().toString();
            String instStr = etInstallmentsCount.getText().toString().trim();
            String amtType = spAmountType.getText().toString();
            String date = etDate.getText().toString().trim();

            if (name.isEmpty() || instStr.isEmpty() || date.isEmpty() || freq.isEmpty() || amtType.isEmpty()) {
                Toast.makeText(activity, "Fill in all basic fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalInst = Integer.parseInt(instStr);
            ArrayList<Double> amountsArray = new ArrayList<>();

            if (amtType.equals("Fixed Amount")) {
                if (etAmount.getText().toString().trim().isEmpty()) {
                    Toast.makeText(activity, "Please specify an installment amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double fixedVal = Double.parseDouble(etAmount.getText().toString().trim());
                for(int k=0; k<totalInst; k++) amountsArray.add(fixedVal);
            } else {
                for (TextInputEditText field : dynamicAmountFields) amountsArray.add(Double.parseDouble(field.getText().toString().trim()));
            }

            Map<String, Object> chitPayload = new HashMap<>();
            chitPayload.put("name", name); chitPayload.put("frequency", freq); chitPayload.put("installments", totalInst);
            chitPayload.put("amount_type", amtType); chitPayload.put("startDate", date); chitPayload.put("amounts", amountsArray);

            activity.firestore.collection("chits").add(chitPayload).addOnSuccessListener(docRef -> {
                String newId = docRef.getId();
                for (TextInputEditText field : dynamicMemberFields) {
                    String mName = field.getText().toString().trim();
                    if(!mName.isEmpty()){
                        Map<String, Object> mPayload = new HashMap<>();
                        mPayload.put("chitId", newId); mPayload.put("name", mName);
                        activity.firestore.collection("members").add(mPayload);
                    }
                }
                Toast.makeText(activity, "Chit Synchronized to Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss(); activity.chitId = newId;
                activity.syncCurrentChitContextFromCloud();
            });
        });
    }

    private void triggerDynamicAmountFields(String countStr, LinearLayout container, ArrayList<TextInputEditText> fieldTrackerList) {
        container.removeAllViews(); fieldTrackerList.clear();
        if (!countStr.trim().isEmpty()) {
            int total = Integer.parseInt(countStr.trim());
            for (int i = 1; i <= total; i++) {
                TextInputLayout wrap = new TextInputLayout(activity);
                wrap.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                wrap.setBoxCornerRadii(16f, 16f, 16f, 16f); 
                wrap.setBoxBackgroundColor(Color.TRANSPARENT); 
                wrap.setHint("Installment " + i + " Amount (₹)");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 24); 
                wrap.setLayoutParams(lp);

                TextInputEditText etAmtInput = new TextInputEditText(wrap.getContext());
                etAmtInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                etAmtInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                wrap.addView(etAmtInput); 
                container.addView(wrap); 
                fieldTrackerList.add(etAmtInput);
            }
        }
    }

    public void showAddNotesDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LinearLayout wrapperLayout = new LinearLayout(activity);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.setPadding(60, 60, 60, 20);

        TextInputLayout tlNote = new TextInputLayout(activity);
        tlNote.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tlNote.setBoxCornerRadii(16f, 16f, 16f, 16f);
        tlNote.setBoxBackgroundColor(Color.TRANSPARENT);
        tlNote.setHint("Write a new pinned note...");

        TextInputEditText etNote = new TextInputEditText(tlNote.getContext());
        etNote.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        etNote.setTypeface(Typeface.MONOSPACE);

        tlNote.addView(etNote);
        wrapperLayout.addView(tlNote);

        Button btnAdd = new Button(activity);
        btnAdd.setText("Add to Carousel");
        btnAdd.setTextColor(Color.WHITE); 
        btnAdd.setAllCaps(false); 
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(Color.parseColor("#0F172A")); 
        btnBg.setCornerRadius(24f); 
        btnAdd.setBackground(btnBg);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 20, 0, 40);
        btnAdd.setLayoutParams(btnParams);
        wrapperLayout.addView(btnAdd);

        TextView tvListHeader = new TextView(activity);
        tvListHeader.setText("Saved Notes (Long-press to delete)");
        tvListHeader.setTextSize(14);
        tvListHeader.setTextColor(Color.parseColor("#64748B"));
        tvListHeader.setPadding(0, 0, 0, 20);
        wrapperLayout.addView(tvListHeader);

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout notesListContainer = new LinearLayout(activity);
        notesListContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(notesListContainer);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500); 
        scrollView.setLayoutParams(scrollParams);
        wrapperLayout.addView(scrollView);

        builder.setView(wrapperLayout);
        builder.setTitle("Manage Pinned Notes");
        builder.setPositiveButton("Close Dashboard", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);

        android.content.SharedPreferences prefs = activity.getSharedPreferences("ChitPrefs", Context.MODE_PRIVATE);

        Runnable renderNotesList = new Runnable() {
            @Override
            public void run() {
                notesListContainer.removeAllViews();
                java.util.Set<String> existingNotes = prefs.getStringSet("global_notes_set", new java.util.HashSet<>());
                
                if (existingNotes.isEmpty()) {
                    TextView empty = new TextView(activity);
                    empty.setText("No notes added yet.");
                    empty.setTextColor(Color.parseColor("#94A3B8"));
                    notesListContainer.addView(empty);
                    return;
                }

                for (String noteStr : existingNotes) {
                    TextView tv = new TextView(activity);
                    tv.setText("• " + noteStr);
                    tv.setPadding(30, 30, 30, 30);
                    tv.setTextColor(Color.parseColor("#1E293B"));
                    tv.setTypeface(Typeface.MONOSPACE);
                    
                    android.graphics.drawable.GradientDrawable noteBg = new android.graphics.drawable.GradientDrawable();
                    noteBg.setColor(Color.parseColor("#F1F5F9"));
                    noteBg.setCornerRadius(20f);
                    tv.setBackground(noteBg);
                    
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 15);
                    tv.setLayoutParams(lp);

                    tv.setOnLongClickListener(v -> {
                        new MaterialAlertDialogBuilder(activity)
                            .setTitle("Delete Note")
                            .setMessage("Are you sure you want to permanently delete this note?\n\n\"" + noteStr + "\"")
                            .setPositiveButton("Delete Permanently", (d, w) -> {
                                java.util.HashSet<String> updatedNotes = new java.util.HashSet<>(prefs.getStringSet("global_notes_set", new java.util.HashSet<>()));
                                updatedNotes.remove(noteStr);
                                prefs.edit().putStringSet("global_notes_set", updatedNotes).apply();
                                
                                this.run(); 
                                activity.refreshGlobalNoteCard(); 
                                Toast.makeText(activity, "Note deleted permanently", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                        return true;
                    });
                    notesListContainer.addView(tv);
                }
            }
        };

        renderNotesList.run(); 

        btnAdd.setOnClickListener(v -> {
            String noteText = etNote.getText().toString().trim();
            if (!noteText.isEmpty()) {
                java.util.Set<String> existingNotes = prefs.getStringSet("global_notes_set", new java.util.HashSet<>());
                java.util.HashSet<String> updatedNotes = new java.util.HashSet<>(existingNotes);
                updatedNotes.add(noteText);
                
                prefs.edit().putStringSet("global_notes_set", updatedNotes).apply();
                
                etNote.setText(""); 
                renderNotesList.run(); 
                activity.refreshGlobalNoteCard(); 
                Toast.makeText(activity, "Note added to Carousel!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showDeleteChitSelectionDialog() {
        if (activity.globalChitsList == null || activity.globalChitsList.isEmpty()) {
            Toast.makeText(activity, "No Chit Fund groups available to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] chitNames = new String[activity.globalChitsList.size()];
        for (int i = 0; i < activity.globalChitsList.size(); i++) {
            chitNames[i] = activity.globalChitsList.get(i).name;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Select Chit Group to Delete")
                .setItems(chitNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LedgerComponents.CloudChitItem chosenChit = activity.globalChitsList.get(which);
                        showFinalDeleteConfirmationDialog(chosenChit.id, chosenChit.name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFinalDeleteConfirmationDialog(final String targetedDeleteId, String chitName) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Delete \"" + chitName + "\"?")
                .setMessage("Are you sure you want to permanently delete this group? All ledger logs, member lists, payments, and advances will be completely wiped from the cloud.")
                .setPositiveButton("Delete Permanently", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeCloudChitDeletion(targetedDeleteId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeCloudChitDeletion(final String targetedDeleteId) {
        activity.firestore.collection("chits").document(targetedDeleteId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Chit Group deleted successfully!", Toast.LENGTH_SHORT).show();
                    
                    if (targetedDeleteId.equals(activity.chitId)) {
                        activity.chitId = null;
                        activity.globalMembersList.clear();
                        activity.tlFundTable.removeAllViews();
                        activity.tvFundTitle.setText("No active Chit Fund found. Create one using the menu!");
                        activity.llFormContainer.setVisibility(View.GONE);
                    }

                    activity.firestore.collection("members").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });

                    activity.firestore.collection("payments").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });

                    activity.firestore.collection("advances").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
