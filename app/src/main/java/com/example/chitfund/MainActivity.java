package com.example.chitfund;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

// FIREBASE FIRESTORE REAL-TIME IMPORTS
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private String chitId = null; 
    private String historyFilterChitId = "ALL"; 

    private AutoCompleteTextView spChitSelector;
    private AutoCompleteTextView spMembers;
    private AutoCompleteTextView spHistoryFilter;
    private Button btnSelectInstallments;
    private Button btnToggleMatrixOrientation;
    private TableLayout tlFundTable;
    private TableLayout tlAdvancesTable;
    private TableLayout tlGlobalSummaryTable; 
    private LinearLayout llGlobalSummaryContainer; 
    private TextView tvFundTitle;
    private View llFormContainer;
    
    private TextView tvHistorySummary;
    private TableLayout tlHistoryTable;

    private View tabContainerMatrix;
    private View tabContainerCollect;
    private View tabContainerLedger;
    private View tabContainerAdvances;
    
    private int totalInstallmentsCount;
    private String frequencyType;
    private String firstInstallmentDateStr;

    private boolean isMatrixVertical = false;

    // HIGH-SPEED CACHE LOOKUP FIELDS FOR REAL-TIME RENDERING
    private HashSet<String> globalPaymentsCache = new HashSet<>(); 
    private HashMap<String, ArrayList<String>> globalChitMembersCache = new HashMap<>(); 
    private HashMap<String, Integer> globalAdvanceStartCache = new HashMap<>(); 
    private HashMap<String, Double> globalAdvanceRateCache = new HashMap<>(); 
    private HashMap<String, String> globalAdvanceDateCache = new HashMap<>(); 
    
    private HashMap<String, Double> globalChitTotalAdvancesCache = new HashMap<>();
    
    private HashMap<String, ArrayList<Double>> globalChitAmountsCache = new HashMap<>();
    private HashMap<String, String> globalChitStartDatesCache = new HashMap<>();
    private HashMap<String, String> globalChitFrequenciesCache = new HashMap<>();
    private HashMap<String, Integer> globalChitInstallmentsCountCache = new HashMap<>();
    private ArrayList<Double> baseChitInstallmentAmounts = new ArrayList<>(); 

    private ArrayList<android.animation.ValueAnimator> activeSnakeAnimators = new ArrayList<>();
    private android.animation.ValueAnimator globalSummaryAnimator = null; 
    private ArrayList<Integer> selectedInstallmentsList = new ArrayList<>();
    
    public static class CloudChitItem {
        public String id;
        public String name;
        public CloudChitItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    
    private ArrayList<CloudChitItem> globalChitsList = new ArrayList<>();
    private ArrayList<String> globalMembersList = new ArrayList<>();

    // Play Store style morphing cursive progress animation snake engine
    private static class SnakeBorderDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint borderPaint;
        private final android.graphics.Paint fillPaint;
        private final android.graphics.Path borderPath;
        private final float cornerRadius;
        private float animationProgress = 0f;

        public SnakeBorderDrawable(int strokeColor, int baseBgColor, float cornerRadius) {
            this.cornerRadius = cornerRadius;
            this.borderPath = new android.graphics.Path();

            fillPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(android.graphics.Paint.Style.FILL);
            fillPaint.setColor(baseBgColor);

            borderPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(android.graphics.Paint.Style.STROKE);
            borderPaint.setStrokeWidth(6f); 
            borderPaint.setColor(strokeColor);
            borderPaint.setStrokeCap(android.graphics.Paint.Cap.ROUND); 
        }

        public void setAnimationProgress(float progress) {
            this.animationProgress = progress;
            invalidateSelf();
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            float inset = borderPaint.getStrokeWidth() / 2f;
            android.graphics.RectF rectF = new android.graphics.RectF(bounds);
            rectF.inset(inset, inset);
            
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint);
            borderPath.reset();
            borderPath.addRoundRect(rectF, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW);

            android.graphics.PathMeasure pathMeasure = new android.graphics.PathMeasure(borderPath, false);
            float totalPerimeterLength = pathMeasure.getLength();
            float sinePulseFactor = (float) Math.sin(animationProgress * Math.PI * 2.0); 
            float visibleSnakeBodySize = totalPerimeterLength * (0.15f + (0.10f * sinePulseFactor));
            float infiniteGapRemainder = totalPerimeterLength - visibleSnakeBodySize;

            borderPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{visibleSnakeBodySize, infiniteGapRemainder}, 
                animationProgress * totalPerimeterLength
            ));
            canvas.drawPath(borderPath, borderPaint);
        }

        @Override public void setAlpha(int alpha) { borderPaint.setAlpha(alpha); fillPaint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { borderPaint.setColorFilter(cf); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    @Override
    protected void onCreate(Bundle Bundle) {
        super.onCreate(Bundle);
        setContentView(R.layout.activity_main);
        
        firestore = FirebaseFirestore.getInstance();

        spChitSelector = findViewById(R.id.spChitSelector);
        spMembers = findViewById(R.id.spMembers);
        spHistoryFilter = findViewById(R.id.spHistoryFilter);
        btnSelectInstallments = findViewById(R.id.btnSelectInstallments);
        btnToggleMatrixOrientation = findViewById(R.id.btnToggleMatrixOrientation);
        Button btnAddInstallment = findViewById(R.id.btnAddInstallment);
        tlFundTable = findViewById(R.id.tlFundTable);
        tlAdvancesTable = findViewById(R.id.tlAdvancesTable);
        tlGlobalSummaryTable = findViewById(R.id.tlGlobalSummaryTable);
        llGlobalSummaryContainer = findViewById(R.id.llGlobalSummaryContainer);
        tvFundTitle = findViewById(R.id.tvFundTitle);
        llFormContainer = findViewById(R.id.llFormContainer);
        
        tvHistorySummary = findViewById(R.id.tvHistorySummary);
        tlHistoryTable = findViewById(R.id.tlHistoryTable);

        tabContainerMatrix = findViewById(R.id.tabContainerMatrix);
        tabContainerCollect = findViewById(R.id.tabContainerCollect);
        tabContainerLedger = findViewById(R.id.tabContainerLedger);
        tabContainerAdvances = findViewById(R.id.tabContainerAdvances);

        float radiusPx = 24 * getResources().getDisplayMetrics().density;
        final SnakeBorderDrawable globalSnakeDrawable = new SnakeBorderDrawable(Color.parseColor("#10B981"), Color.parseColor("#F0FDF4"), radiusPx);
        llGlobalSummaryContainer.setBackground(globalSnakeDrawable);

        globalSummaryAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        globalSummaryAnimator.setDuration(1600); 
        globalSummaryAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        globalSummaryAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        globalSummaryAnimator.addUpdateListener(animation -> {
            globalSnakeDrawable.setAnimationProgress(-(float) animation.getAnimatedValue());
            llGlobalSummaryContainer.postInvalidateOnAnimation();
        });
        globalSummaryAnimator.start();

        TabLayout tabLayout = findViewById(R.id.premiumTabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Collect"));
        tabLayout.addTab(tabLayout.newTab().setText("Matrix Grid"));
        tabLayout.addTab(tabLayout.newTab().setText("Ledger"));
        tabLayout.addTab(tabLayout.newTab().setText("Advances"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    tabContainerCollect.setVisibility(View.VISIBLE);
                    tabContainerMatrix.setVisibility(View.GONE);
                    tabContainerLedger.setVisibility(View.GONE);
                    tabContainerAdvances.setVisibility(View.GONE);
                } else if (position == 1) {
                    tabContainerCollect.setVisibility(View.GONE);
                    tabContainerMatrix.setVisibility(View.VISIBLE);
                    tabContainerLedger.setVisibility(View.GONE);
                    tabContainerAdvances.setVisibility(View.GONE);
                    refreshFundMatrixTable();
                } else if (position == 2) {
                    tabContainerCollect.setVisibility(View.GONE);
                    tabContainerMatrix.setVisibility(View.GONE);
                    tabContainerLedger.setVisibility(View.VISIBLE);
                    tabContainerAdvances.setVisibility(View.GONE);
                } else {
                    tabContainerCollect.setVisibility(View.GONE);
                    tabContainerMatrix.setVisibility(View.GONE);
                    tabContainerLedger.setVisibility(View.GONE);
                    tabContainerAdvances.setVisibility(View.VISIBLE);
                    refreshAdvancesTable();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        spChitSelector.setOnItemClickListener((parent, view, position, id) -> {
            CloudChitItem selected = globalChitsList.get(position);
            if (selected != null && !selected.id.equals(chitId)) {
                chitId = selected.id;
                syncCurrentChitContextFromCloud();
            }
        });

        spHistoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                historyFilterChitId = "ALL";
            } else {
                historyFilterChitId = globalChitsList.get(position - 1).id;
            }
            refreshTransactionHistory();
        });

        btnToggleMatrixOrientation.setOnClickListener(v -> {
            isMatrixVertical = !isMatrixVertical;
            if (isMatrixVertical) {
                btnToggleMatrixOrientation.setText("View Horizontally (Scroll Right)");
            } else {
                btnToggleMatrixOrientation.setText("View Vertically (Scroll Down)");
            }
            refreshFundMatrixTable();
        });

        btnSelectInstallments.setOnClickListener(v -> showMultiSelectInstallmentsDialog());

        initGlobalDatabaseSynchronizers();
        refreshTransactionHistory();

        btnAddInstallment.setOnClickListener(v -> {
            if (chitId == null) {
                Toast.makeText(MainActivity.this, "Please create a Chit Fund group first!", Toast.LENGTH_SHORT).show();
                return;
            }
            String TylerMember = spMembers.getText().toString().trim();
            if (TylerMember.isEmpty() || !globalMembersList.contains(TylerMember)) {
                Toast.makeText(MainActivity.this, "Please select a valid member!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedInstallmentsList.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please select at least one installment!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            for (int instNum : selectedInstallmentsList) {
                String lookupKey = chitId + "_" + TylerMember + "_" + instNum;
                if (!globalPaymentsCache.contains(lookupKey)) {
                    double currentTargetAmount = getSpecificCachedMemberInstallmentAmount(chitId, TylerMember, instNum);
                    
                    Map<String, Object> paymentPayload = new HashMap<>();
                    paymentPayload.put("chitId", chitId);
                    paymentPayload.put("installment_num", instNum);
                    paymentPayload.put("member_name", TylerMember);
                    paymentPayload.put("amount", currentTargetAmount);
                    paymentPayload.put("date", currentDate);
                    paymentPayload.put("timestamp", System.currentTimeMillis());

                    firestore.collection("payments").add(paymentPayload);
                    globalPaymentsCache.add(lookupKey);
                }
            }

            Toast.makeText(MainActivity.this, "Installments Saved Online!", Toast.LENGTH_SHORT).show();
            resetInstallmentSelection();
            refreshFundMatrixTable();
            refreshTransactionHistory();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_new_chit) { showNewChitDialog(); return true; }
        if (item.getItemId() == R.id.menu_log_advance) { showLogAdvanceDialog(); return true; }
        if (item.getItemId() == R.id.menu_delete_chit) { showDeleteChitSelectionDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void initGlobalDatabaseSynchronizers() {
        firestore.collection("chits").addSnapshotListener((value, error) -> {
            if (value == null) return;
            globalChitsList.clear();
            globalChitStartDatesCache.clear();
            globalChitFrequenciesCache.clear();
            globalChitInstallmentsCountCache.clear();
            globalChitAmountsCache.clear();

            for (QueryDocumentSnapshot doc : value) {
                String id = doc.getId();
                globalChitsList.add(new CloudChitItem(id, doc.getString("name")));
                globalChitStartDatesCache.put(id, doc.getString("startDate"));
                globalChitFrequenciesCache.put(id, doc.getString("frequency"));
                globalChitInstallmentsCountCache.put(id, doc.getLong("installments").intValue());
                globalChitAmountsCache.put(id, (ArrayList<Double>) doc.get("amounts"));
            }
            rebuildGlobalDropdownsUI();
            
            firestore.collection("members").addSnapshotListener((mVal, mErr) -> {
                if (mVal == null) return;
                globalChitMembersCache.clear();
                for (QueryDocumentSnapshot mDoc : mVal) {
                    String cId = mDoc.getString("chitId");
                    String name = mDoc.getString("name");
                    if (!globalChitMembersCache.containsKey(cId)) globalChitMembersCache.put(cId, new ArrayList<>());
                    globalChitMembersCache.get(cId).add(name);
                }

                firestore.collection("advances").addSnapshotListener((aVal, aErr) -> {
                    if (aVal == null) return;
                    globalAdvanceStartCache.clear();
                    globalAdvanceRateCache.clear();
                    globalAdvanceDateCache.clear();
                    globalChitTotalAdvancesCache.clear();
                    
                    for (QueryDocumentSnapshot aDoc : aVal) {
                        String cId = aDoc.getString("chitId");
                        String compositeKey = cId + "_" + aDoc.getString("member_name");
                        globalAdvanceStartCache.put(compositeKey, aDoc.getLong("installment_num").intValue());
                        globalAdvanceRateCache.put(compositeKey, aDoc.getDouble("new_amount"));
                        globalAdvanceDateCache.put(compositeKey, aDoc.getString("date")); 
                        
                        double advAmount = aDoc.getDouble("advance_amount") != null ? aDoc.getDouble("advance_amount") : 0.0;
                        globalChitTotalAdvancesCache.put(cId, globalChitTotalAdvancesCache.getOrDefault(cId, 0.0) + advAmount);
                    }

                    firestore.collection("payments").addSnapshotListener((pVal, pErr) -> {
                        if (pVal == null) return;
                        globalPaymentsCache.clear();
                        for (QueryDocumentSnapshot pDoc : pVal) {
                            String compositeKey = pDoc.getString("chitId") + "_" + pDoc.getString("member_name") + "_" + pDoc.getLong("installment_num").intValue();
                            globalPaymentsCache.add(compositeKey);
                        }
                        
                        calculateGlobalMonthlyDuesEngine();
                        syncCurrentChitContextFromCloud();
                    });
                });
            });
        });
    }

    private void rebuildGlobalDropdownsUI() {
        ArrayList<String> filterOptions = new ArrayList<>();
        filterOptions.add("All Chits");
        for (CloudChitItem item : globalChitsList) filterOptions.add(item.name);
        spHistoryFilter.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, filterOptions));

        ArrayAdapter<CloudChitItem> adapter = new ArrayAdapter<>(this, R.layout.list_item_premium, globalChitsList);
        spChitSelector.setAdapter(adapter);

        if (!globalChitsList.isEmpty() && chitId == null) {
            spChitSelector.setText(globalChitsList.get(0).name, false);
            chitId = globalChitsList.get(0).id;
        }
    }

    private void calculateGlobalMonthlyDuesEngine() {
        tlGlobalSummaryTable.removeAllViews();
        if (globalChitsList.isEmpty()) return;

        android.graphics.drawable.GradientDrawable rowLine = new android.graphics.drawable.GradientDrawable();
        rowLine.setColor(Color.parseColor("#E2E8F0"));
        rowLine.setSize(2, 2);
        tlGlobalSummaryTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
        tlGlobalSummaryTable.setDividerDrawable(rowLine);

        TableRow header = new TableRow(this);
        header.setBackgroundResource(R.drawable.table_header_bg); 
        header.setPadding(4, 12, 4, 12);
        
        String[] headers = {"Chit Group Name", "Current Month Inst.", "Current Month Pending", "Previous Pending", "Total Outstanding"};
        for (String col : headers) {
            TextView tv = new TextView(this); tv.setText(col); tv.setPadding(20, 12, 20, 12);
            tv.setTextColor(Color.WHITE); tv.setTextSize(13); tv.setTypeface(null, Typeface.BOLD); 
            tv.setGravity(col.equals("Chit Group Name") ? Gravity.START : Gravity.CENTER);
            header.addView(tv);
        }
        tlGlobalSummaryTable.addView(header);

        double aggregateCurrentPending = 0.0;
        double aggregatePreviousPending = 0.0;
        Calendar todayCal = Calendar.getInstance();

        for (CloudChitItem item : globalChitsList) {
            String id = item.id;
            if (!globalChitStartDatesCache.containsKey(id)) continue;

            String startStr = globalChitStartDatesCache.get(id);
            String freq = globalChitFrequenciesCache.get(id);
            int maxInst = globalChitInstallmentsCountCache.get(id);
            ArrayList<String> members = globalChitMembersCache.get(id);
            if (members == null) members = new ArrayList<>();

            double currentMonthChitPending = 0.0;
            double previousArrearsChitPending = 0.0;
            boolean hasMilestoneThisMonth = false;
            int highestPassedOrCurrentStep = 0;

            double calcTotalPlanAmount = 0.0;
            double calcTotalPaidAmount = 0.0;
            int calcPaidInstCount = 0; 
            ArrayList<Double> dynamicPlanBreakdown = new ArrayList<>();
            ArrayList<Integer> pendingStepsList = new ArrayList<>();
            
            // NEW LIST: Tracks exactly which sequence numbers belong to the current calendar month for Weekly views
            ArrayList<Integer> weeklyStepsThisMonth = new ArrayList<>();

            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startStr);
                Calendar cal = Calendar.getInstance();
                
                for (int step = 1; step <= maxInst; step++) {
                    int idx = step - 1;
                    cal.setTime(d);
                    if ("Monthly".equals(freq)) {
                        cal.add(Calendar.MONTH, idx);
                    } else if ("Half Yearly".equals(freq)) {
                        cal.add(Calendar.MONTH, idx * 6);
                    } else {
                        cal.add(Calendar.DATE, idx * 7);
                    }

                    boolean isPast = false;
                    boolean isCurrent = false;

                    int cY = cal.get(Calendar.YEAR);
                    int tY = todayCal.get(Calendar.YEAR);
                    int cM = cal.get(Calendar.MONTH);
                    int tM = todayCal.get(Calendar.MONTH);

                    // POWERFUL NEW GROUPING ENGINE: Both Weekly and Monthly evaluate against the exact Calendar Month
                    if (cY == tY && cM == tM) {
                        isCurrent = true;
                        hasMilestoneThisMonth = true;
                        if ("Weekly".equals(freq)) {
                            weeklyStepsThisMonth.add(step);
                        }
                    } else if (cY < tY || (cY == tY && cM < tM)) {
                        isPast = true;
                    }

                    if (isCurrent || isPast) {
                        highestPassedOrCurrentStep = step;
                    }

                    double stepExpectedTotal = 0.0;
                    boolean stepIsPending = false;

                    for (String mName : members) {
                        double stepAmt = getSpecificCachedMemberInstallmentAmount(id, mName, step);
                        stepExpectedTotal += stepAmt;

                        String payKey = id + "_" + mName + "_" + step;
                        if (!globalPaymentsCache.contains(payKey)) {
                            if (isCurrent || isPast) stepIsPending = true;
                            if (isCurrent) {
                                currentMonthChitPending += stepAmt;
                            } else if (isPast) {
                                previousArrearsChitPending += stepAmt;
                            }
                        } else {
                            calcTotalPaidAmount += stepAmt;
                            calcPaidInstCount++; 
                        }
                    }
                    
                    dynamicPlanBreakdown.add(stepExpectedTotal);
                    calcTotalPlanAmount += stepExpectedTotal;
                    
                    if (stepIsPending) {
                        pendingStepsList.add(step);
                    }
                }
            } catch (Exception ignored) {}

            // NEW MULTI-FORMATTER: If Weekly, merges (#23, 24, 25, 26). Otherwise, uses the standard #14.
            String displayInstNumberStr = "";
            if ("Weekly".equals(freq) && !weeklyStepsThisMonth.isEmpty()) {
                StringBuilder sb = new StringBuilder("#");
                for(int i=0; i < weeklyStepsThisMonth.size(); i++) {
                    sb.append(weeklyStepsThisMonth.get(i));
                    if(i < weeklyStepsThisMonth.size() - 1) sb.append(", ");
                }
                displayInstNumberStr = sb.toString();
            } else {
                int displayInstNumber = hasMilestoneThisMonth ? highestPassedOrCurrentStep : Math.min(highestPassedOrCurrentStep + 1, maxInst);
                displayInstNumberStr = "#" + displayInstNumber;
            }

            if (!hasMilestoneThisMonth && previousArrearsChitPending == 0) {
                continue; 
            }

            double totalChitOutstanding = currentMonthChitPending + previousArrearsChitPending;
            aggregateCurrentPending += currentMonthChitPending;
            aggregatePreviousPending += previousArrearsChitPending;
            
            TableRow row = new TableRow(this);
            row.setPadding(4, 10, 4, 10);
            row.setBackgroundColor(Color.parseColor("#FFF7ED"));

            TextView tvName = new TextView(this); tvName.setText(item.name); tvName.setPadding(20, 12, 20, 12); tvName.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            
            if (previousArrearsChitPending > 0) {
                tvName.setTextColor(Color.parseColor("#DC2626")); 
            } else if (currentMonthChitPending > 0) {
                tvName.setTextColor(Color.parseColor("#0F172A")); 
            } else {
                tvName.setTextColor(Color.parseColor("#15803D")); 
            } 

            final double totalAdvancesTaken = globalChitTotalAdvancesCache.containsKey(id) ? globalChitTotalAdvancesCache.get(id) : 0.0;
            final String targetName = item.name;
            final String targetStartDate = startStr;
            final String targetFreq = freq;
            final int targetMaxInst = maxInst;
            final ArrayList<String> targetMembers = members;
            final double curMonthDues = currentMonthChitPending;
            final double pastMonthDues = previousArrearsChitPending;
            final double grossDues = totalChitOutstanding;
            final double calcBalanceToPay = calcTotalPlanAmount - calcTotalPaidAmount;

            ArrayList<String> advanceLogsList = new ArrayList<>();
            for (String mName : members) {
                String advKey = id + "_" + mName;
                if (globalAdvanceStartCache.containsKey(advKey)) {
                    int aStep = globalAdvanceStartCache.get(advKey);
                    String aDate = globalAdvanceDateCache.containsKey(advKey) ? globalAdvanceDateCache.get(advKey) : "Unknown Date";
                    advanceLogsList.add(mName + " took Advance at Step #" + aStep + " on " + aDate);
                }
            }

            final double targetPlanAmount = calcTotalPlanAmount;
            final double targetPaidAmount = calcTotalPaidAmount;
            final double targetBalance = calcBalanceToPay;
            final int targetPaidInstCount = calcPaidInstCount;
            final int targetRemainingInstCount = (targetMaxInst * targetMembers.size()) - calcPaidInstCount; 
            
            final ArrayList<Double> targetPlanBreakdownList = dynamicPlanBreakdown;
            final ArrayList<Integer> targetPendingSteps = pendingStepsList;
            final ArrayList<String> targetAdvanceLogs = advanceLogsList;
            final String targetActiveInstStr = displayInstNumberStr;

            tvName.setOnClickListener(v -> {
                showPremiumChitSummaryDialog(
                    targetName, targetStartDate, targetFreq, targetMaxInst, targetActiveInstStr, 
                    targetMembers, curMonthDues, pastMonthDues, grossDues, totalAdvancesTaken, 
                    targetPlanBreakdownList, targetPendingSteps, targetPlanAmount, targetPaidAmount, 
                    targetBalance, targetAdvanceLogs, targetPaidInstCount, targetRemainingInstCount
                );
            });

            row.addView(tvName);
            
            TextView tvInst = new TextView(this); tvInst.setText(displayInstNumberStr); tvInst.setPadding(20, 12, 20, 12); tvInst.setGravity(Gravity.CENTER); tvInst.setTextColor(Color.parseColor("#475569")); row.addView(tvInst);
            TextView tvCur = new TextView(this); tvCur.setText("₹" + String.format(Locale.getDefault(), "%.1f", currentMonthChitPending)); tvCur.setPadding(20, 12, 20, 12); tvCur.setGravity(Gravity.CENTER); tvCur.setTextColor(Color.parseColor("#1E293B")); row.addView(tvCur);
            TextView tvPrev = new TextView(this); tvPrev.setText("₹" + String.format(Locale.getDefault(), "%.1f", previousArrearsChitPending)); tvPrev.setPadding(20, 12, 20, 12); tvPrev.setGravity(Gravity.CENTER); tvPrev.setTextColor(previousArrearsChitPending > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#64748B")); if(previousArrearsChitPending > 0) tvPrev.setTypeface(null, Typeface.BOLD); row.addView(tvPrev);
            TextView tvTot = new TextView(this); tvTot.setText("₹" + String.format(Locale.getDefault(), "%.1f", totalChitOutstanding)); tvTot.setPadding(20, 12, 20, 12); tvTot.setGravity(Gravity.CENTER); tvTot.setTextColor(Color.parseColor("#0F172A")); tvTot.setTypeface(null, Typeface.BOLD); row.addView(tvTot); 

            tlGlobalSummaryTable.addView(row);
        }

        TableRow footerRow = new TableRow(this);
        footerRow.setBackgroundResource(R.drawable.table_footer_bg); 
        footerRow.setPadding(4, 12, 4, 12);

        TextView tvTotalLbl = new TextView(this); tvTotalLbl.setText("GRAND TOTALS"); tvTotalLbl.setPadding(20, 12, 20, 12); tvTotalLbl.setTextColor(Color.parseColor("#0F172A")); tvTotalLbl.setTypeface(null, Typeface.BOLD); footerRow.addView(tvTotalLbl);
        TextView tvEmpty = new TextView(this); tvEmpty.setText("-"); tvEmpty.setPadding(20, 12, 20, 12); tvEmpty.setGravity(Gravity.CENTER); tvEmpty.setTextColor(Color.TRANSPARENT); footerRow.addView(tvEmpty);
        TextView tvSumCur = new TextView(this); tvSumCur.setText("₹" + String.format(Locale.getDefault(), "%.1f", aggregateCurrentPending)); tvSumCur.setPadding(20, 12, 20, 12); tvSumCur.setGravity(Gravity.CENTER); tvSumCur.setTextColor(Color.parseColor("#15803D")); tvSumCur.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumCur);
        TextView tvSumPrev = new TextView(this); tvSumPrev.setText("₹" + String.format(Locale.getDefault(), "%.1f", aggregatePreviousPending)); tvSumPrev.setPadding(20, 12, 20, 12); tvSumPrev.setGravity(Gravity.CENTER); tvSumPrev.setTextColor(Color.parseColor("#B91C1C")); tvSumPrev.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumPrev);
        TextView tvSumGrand = new TextView(this); tvSumGrand.setText("₹" + String.format(Locale.getDefault(), "%.1f", (aggregateCurrentPending + aggregatePreviousPending))); tvSumGrand.setPadding(20, 12, 20, 12); tvSumGrand.setGravity(Gravity.CENTER); tvSumGrand.setTextColor(Color.parseColor("#0F172A")); tvSumGrand.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumGrand);

        tlGlobalSummaryTable.addView(footerRow);
    }

    private void showPremiumChitSummaryDialog(String name, String startDate, String freq, int maxInst, String activeInstStr, ArrayList<String> members, double curDues, double pastDues, double grossDues, double totalAdvances, ArrayList<Double> planBreakdown, ArrayList<Integer> pendingSteps, double totalPlanAmount, double totalPaid, double balanceAmount, ArrayList<String> advanceLogs, int paidInstCount, int remainingInstCount) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(60, 60, 60, 60);
        scrollView.addView(mainLayout);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(name);
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        mainLayout.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Workspace Overview Summary");
        tvSubtitle.setTextSize(14);
        tvSubtitle.setTextColor(Color.parseColor("#64748B"));
        tvSubtitle.setPadding(0, 0, 0, 50);
        mainLayout.addView(tvSubtitle);

        float cornerRadius = 32f;
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 30);

        // 1. TOTAL PENDING DUES (Combines Active & Arrears w/ Pending Step Identifiers)
        LinearLayout pendingCard = new LinearLayout(this);
        pendingCard.setOrientation(LinearLayout.VERTICAL);
        pendingCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable pendBg = new android.graphics.drawable.GradientDrawable();
        pendBg.setColor(Color.parseColor("#EFF6FF")); // Soft Indigo Blue
        pendBg.setCornerRadius(cornerRadius);
        pendingCard.setBackground(pendBg);
        pendingCard.setLayoutParams(cardParams);
        
        TextView tvPendLbl = new TextView(this);
        tvPendLbl.setText("Total Pending Dues");
        tvPendLbl.setTextColor(Color.parseColor("#1D4ED8"));
        tvPendLbl.setTextSize(13);
        tvPendLbl.setTypeface(null, Typeface.BOLD);
        
        TextView tvPendVal = new TextView(this);
        tvPendVal.setText("₹" + String.format(Locale.getDefault(), "%.1f", grossDues));
        tvPendVal.setTextColor(Color.parseColor("#1E3A8A"));
        tvPendVal.setTextSize(26);
        tvPendVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        
        pendingCard.addView(tvPendLbl);
        pendingCard.addView(tvPendVal);
        
        TextView tvPendSteps = new TextView(this);
        String pStepsStr = pendingSteps.isEmpty() ? "None" : pendingSteps.toString().replace("[", "").replace("]", "");
        tvPendSteps.setText("Pending Installments: " + pStepsStr);
        tvPendSteps.setTextColor(Color.parseColor("#2563EB"));
        tvPendSteps.setTextSize(13);
        tvPendSteps.setPadding(0, 10, 0, 0);
        pendingCard.addView(tvPendSteps);

        mainLayout.addView(pendingCard);

        // 2. FINANCIAL SPLIT GRID (Active Dues vs Past Arrears)
        LinearLayout finGrid = new LinearLayout(this);
        finGrid.setOrientation(LinearLayout.HORIZONTAL);
        finGrid.setWeightSum(2);
        finGrid.setLayoutParams(cardParams);

        LinearLayout curCard = new LinearLayout(this);
        curCard.setOrientation(LinearLayout.VERTICAL);
        curCard.setPadding(40, 40, 40, 40);
        android.graphics.drawable.GradientDrawable curBg = new android.graphics.drawable.GradientDrawable();
        curBg.setColor(Color.parseColor("#F1F5F9")); 
        curBg.setCornerRadius(cornerRadius);
        curCard.setBackground(curBg);
        
        TextView tvCurLbl = new TextView(this);
        tvCurLbl.setText("Active Dues");
        tvCurLbl.setTextColor(Color.parseColor("#475569"));
        tvCurLbl.setTextSize(12);
        TextView tvCurVal = new TextView(this);
        tvCurVal.setText("₹" + String.format(Locale.getDefault(), "%.1f", curDues));
        tvCurVal.setTextColor(Color.parseColor("#0F172A"));
        tvCurVal.setTextSize(18);
        tvCurVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        curCard.addView(tvCurLbl);
        curCard.addView(tvCurVal);
        
        LinearLayout.LayoutParams halfLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        halfLeft.setMargins(0, 0, 15, 0);
        curCard.setLayoutParams(halfLeft);
        finGrid.addView(curCard);

        LinearLayout pastCard = new LinearLayout(this);
        pastCard.setOrientation(LinearLayout.VERTICAL);
        pastCard.setPadding(40, 40, 40, 40);
        android.graphics.drawable.GradientDrawable pastBg = new android.graphics.drawable.GradientDrawable();
        pastBg.setColor(Color.parseColor("#FFF7ED")); 
        pastBg.setCornerRadius(cornerRadius);
        pastCard.setBackground(pastBg);

        TextView tvPastLbl = new TextView(this);
        tvPastLbl.setText("Past Arrears");
        tvPastLbl.setTextColor(Color.parseColor("#C2410C"));
        tvPastLbl.setTextSize(12);
        TextView tvPastVal = new TextView(this);
        tvPastVal.setText("₹" + String.format(Locale.getDefault(), "%.1f", pastDues));
        tvPastVal.setTextColor(Color.parseColor("#EA580C"));
        tvPastVal.setTextSize(18);
        tvPastVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        pastCard.addView(tvPastLbl);
        pastCard.addView(tvPastVal);
        
        LinearLayout.LayoutParams halfRight = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        halfRight.setMargins(15, 0, 0, 0);
        pastCard.setLayoutParams(halfRight);
        finGrid.addView(pastCard);

        mainLayout.addView(finGrid);

        // 3. FINANCIAL FUND OVERVIEW SUMMARY (Plan vs Paid vs Balance + Installment Counts)
        LinearLayout finSumCard = new LinearLayout(this);
        finSumCard.setOrientation(LinearLayout.VERTICAL);
        finSumCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable finSumBg = new android.graphics.drawable.GradientDrawable();
        finSumBg.setColor(Color.parseColor("#F0FDF4")); // Soft Mint Green
        finSumBg.setCornerRadius(cornerRadius);
        finSumCard.setBackground(finSumBg);
        finSumCard.setLayoutParams(cardParams);
        
        String[] finLabels = {"Total Plan Amount", "Total Amount Paid", "Balance to be Paid", "Paid Installments", "Remaining Installments"};
        String[] finValues = {
            "₹" + String.format(Locale.getDefault(), "%.1f", totalPlanAmount),
            "₹" + String.format(Locale.getDefault(), "%.1f", totalPaid),
            "₹" + String.format(Locale.getDefault(), "%.1f", balanceAmount),
            String.valueOf(paidInstCount),
            String.valueOf(remainingInstCount)
        };
        String[] finColors = {"#0F172A", "#15803D", "#B91C1C", "#15803D", "#B91C1C"};

        for(int i=0; i<5; i++){
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);
            
            TextView lbl = new TextView(this);
            lbl.setText(finLabels[i]);
            lbl.setTextColor(Color.parseColor("#475569"));
            lbl.setTextSize(14);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            TextView val = new TextView(this);
            val.setText(finValues[i]);
            val.setTextColor(Color.parseColor(finColors[i]));
            val.setTextSize(15);
            val.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            
            row.addView(lbl);
            row.addView(val);
            finSumCard.addView(row);
        }
        mainLayout.addView(finSumCard);

        // 4. ADVANCES TAKEN HIGHLIGHT CARD & LOGS
        LinearLayout advCard = new LinearLayout(this);
        advCard.setOrientation(LinearLayout.VERTICAL);
        advCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable advBg = new android.graphics.drawable.GradientDrawable();
        advBg.setColor(Color.parseColor("#FEF2F2")); 
        advBg.setCornerRadius(cornerRadius);
        advCard.setBackground(advBg);
        
        TextView tvAdvLbl = new TextView(this);
        tvAdvLbl.setText("Total Advanced Payouts");
        tvAdvLbl.setTextColor(Color.parseColor("#991B1B"));
        tvAdvLbl.setTextSize(13);
        tvAdvLbl.setTypeface(null, Typeface.BOLD);
        
        TextView tvAdvVal = new TextView(this);
        tvAdvVal.setText("₹" + String.format(Locale.getDefault(), "%.1f", totalAdvances));
        tvAdvVal.setTextColor(Color.parseColor("#DC2626"));
        tvAdvVal.setTextSize(26);
        tvAdvVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        
        advCard.addView(tvAdvLbl);
        advCard.addView(tvAdvVal);
        
        if (!advanceLogs.isEmpty()) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#FECACA"));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
            divParams.setMargins(0, 20, 0, 20);
            divider.setLayoutParams(divParams);
            advCard.addView(divider);
            
            for(String log : advanceLogs) {
                TextView tvLog = new TextView(this);
                tvLog.setText("• " + log);
                tvLog.setTextColor(Color.parseColor("#991B1B"));
                tvLog.setTextSize(13);
                advCard.addView(tvLog);
            }
        }
        advCard.setLayoutParams(cardParams);
        mainLayout.addView(advCard);

        // 5. METADATA INFORMATION CONTAINER
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable infoBg = new android.graphics.drawable.GradientDrawable();
        infoBg.setColor(Color.parseColor("#F8FAFC"));
        infoBg.setCornerRadius(cornerRadius);
        infoBg.setStroke(2, Color.parseColor("#E2E8F0"));
        infoLayout.setBackground(infoBg);
        infoLayout.setLayoutParams(cardParams);

        String statusLabel = activeInstStr.contains(",") ? "Steps " + activeInstStr : "Step " + activeInstStr;
        String[] infoLabels = {"Start Date", "Frequency", "Milestones", "Current Status"};
        String[] infoValues = {startDate, freq, maxInst + " Steps", statusLabel};
        for(int i=0; i<4; i++){
            LinearLayout infoRow = new LinearLayout(this);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setPadding(0, 10, 0, 10);
            
            TextView lbl = new TextView(this);
            lbl.setText(infoLabels[i]);
            lbl.setTextColor(Color.parseColor("#64748B"));
            lbl.setTextSize(14);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            TextView val = new TextView(this);
            val.setText(infoValues[i]);
            val.setTextColor(Color.parseColor("#0F172A"));
            val.setTextSize(14);
            val.setTypeface(null, Typeface.BOLD);
            
            infoRow.addView(lbl);
            infoRow.addView(val);
            infoLayout.addView(infoRow);
        }
        mainLayout.addView(infoLayout);

        // 6. REGISTERED MEMBERS LIST 
        TextView tvMemTitle = new TextView(this);
        tvMemTitle.setText("Registered Members (" + members.size() + ")");
        tvMemTitle.setTextSize(15);
        tvMemTitle.setTypeface(null, Typeface.BOLD);
        tvMemTitle.setTextColor(Color.parseColor("#334155"));
        tvMemTitle.setPadding(0, 20, 0, 20);
        mainLayout.addView(tvMemTitle);

        LinearLayout memLayout = new LinearLayout(this);
        memLayout.setOrientation(LinearLayout.VERTICAL);
        memLayout.setPadding(50, 30, 50, 30);
        memLayout.setBackground(infoBg); 
        memLayout.setLayoutParams(cardParams);

        if(members.isEmpty()){
            TextView empty = new TextView(this);
            empty.setText("No members added yet.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            memLayout.addView(empty);
        } else {
            for(int i=0; i<members.size(); i++){
                TextView m = new TextView(this);
                m.setText("• " + members.get(i));
                m.setTextColor(Color.parseColor("#1E293B"));
                m.setTextSize(14);
                m.setPadding(0, 10, 0, 10);
                memLayout.addView(m);
            }
        }
        mainLayout.addView(memLayout);

        // 7. DYNAMIC INSTALLMENT MATRIX 
        TextView tvPlanTitle = new TextView(this);
        tvPlanTitle.setText("Installment Plan Matrix");
        tvPlanTitle.setTextSize(15);
        tvPlanTitle.setTypeface(null, Typeface.BOLD);
        tvPlanTitle.setTextColor(Color.parseColor("#334155"));
        tvPlanTitle.setPadding(0, 20, 0, 20);
        mainLayout.addView(tvPlanTitle);

        LinearLayout planLayout = new LinearLayout(this);
        planLayout.setOrientation(LinearLayout.VERTICAL);
        planLayout.setPadding(50, 30, 50, 30);
        planLayout.setBackground(infoBg);
        planLayout.setLayoutParams(cardParams);

        if(planBreakdown == null || planBreakdown.isEmpty()){
            TextView empty = new TextView(this);
            empty.setText("No plan chart available.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            planLayout.addView(empty);
        } else {
            for(int i=0; i<planBreakdown.size(); i++){
                LinearLayout rRow = new LinearLayout(this);
                rRow.setOrientation(LinearLayout.HORIZONTAL);
                rRow.setPadding(0, 10, 0, 10);
                
                TextView step = new TextView(this);
                step.setText("Step #" + (i+1));
                step.setTextColor(Color.parseColor("#64748B"));
                step.setTextSize(14);
                step.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                
                TextView amt = new TextView(this);
                amt.setText("₹" + String.format(Locale.getDefault(), "%.1f", planBreakdown.get(i)));
                amt.setTextColor(Color.parseColor("#0F172A"));
                amt.setTextSize(14);
                amt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                
                rRow.addView(step);
                rRow.addView(amt);
                planLayout.addView(rRow);
            }
        }
        mainLayout.addView(planLayout);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(scrollView);
        builder.setPositiveButton("Dismiss Dashboard", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
    }

    private double getSpecificCachedMemberInstallmentAmount(String targetChitId, String memberName, int installmentNum) {
        String compositeKey = targetChitId + "_" + memberName;
        if (globalAdvanceStartCache.containsKey(compositeKey)) {
            int startInst = globalAdvanceStartCache.get(compositeKey);
            if (installmentNum > startInst && globalAdvanceRateCache.containsKey(compositeKey)) {
                return globalAdvanceRateCache.get(compositeKey);
            }
        }
        ArrayList<Double> amounts = globalChitAmountsCache.get(targetChitId);
        if (amounts != null && (installmentNum - 1) < amounts.size()) {
            return amounts.get(installmentNum - 1);
        }
        return 0.0;
    }

    private void syncCurrentChitContextFromCloud() {
        if (chitId == null) return;

        firestore.collection("chits").document(chitId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            
            frequencyType = doc.getString("frequency");
            totalInstallmentsCount = doc.getLong("installments").intValue();
            firstInstallmentDateStr = doc.getString("startDate");
            tvFundTitle.setText("Chit Fund Matrix: " + doc.getString("name"));
            
            baseChitInstallmentAmounts = (ArrayList<Double>) doc.get("amounts");
            globalMembersList = globalChitMembersCache.get(chitId);
            if (globalMembersList == null) globalMembersList = new ArrayList<>();

            ArrayAdapter<String> membersAdapter = new ArrayAdapter<>(this, R.layout.list_item_member, globalMembersList);
            spMembers.setAdapter(membersAdapter);
            if (!globalMembersList.isEmpty()) {
                spMembers.setText(globalMembersList.get(0), false);
            } else {
                spMembers.setText("", false);
            }

            resetInstallmentSelection();
            refreshFundMatrixTable();
        });
    }

    private void showDeleteChitSelectionDialog() {
        if (globalChitsList == null || globalChitsList.isEmpty()) {
            Toast.makeText(this, "No Chit Fund groups available to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] chitNames = new String[globalChitsList.size()];
        for (int i = 0; i < globalChitsList.size(); i++) {
            chitNames[i] = globalChitsList.get(i).name;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Chit Group to Delete")
                .setItems(chitNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CloudChitItem chosenChit = globalChitsList.get(which);
                        showFinalDeleteConfirmationDialog(chosenChit.id, chosenChit.name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFinalDeleteConfirmationDialog(final String targetedDeleteId, String chitName) {
        new MaterialAlertDialogBuilder(this)
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
        firestore.collection("chits").document(targetedDeleteId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Chit Group deleted successfully!", Toast.LENGTH_SHORT).show();
                    
                    if (targetedDeleteId.equals(chitId)) {
                        chitId = null;
                        globalMembersList.clear();
                        tlFundTable.removeAllViews();
                        tvFundTitle.setText("No active Chit Fund found. Create one using the menu!");
                        llFormContainer.setVisibility(View.GONE);
                    }

                    firestore.collection("members").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });

                    firestore.collection("payments").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });

                    firestore.collection("advances").whereEqualTo("chitId", targetedDeleteId).get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) { doc.getReference().delete(); }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showMultiSelectInstallmentsDialog() {
        if (chitId == null) return;
        final String member = spMembers.getText().toString().trim();
        if (member.isEmpty()) return;

        final ArrayList<Integer> openInstallmentNumbers = new ArrayList<>();
        ArrayList<String> filteredOptionsList = new ArrayList<>();

        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfDialogOutput = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        for (int i = 1; i <= totalInstallmentsCount; i++) {
            if (!globalPaymentsCache.contains(chitId + "_" + member + "_" + i)) {
                double amt = getSpecificCachedMemberInstallmentAmount(chitId, member, i);
                openInstallmentNumbers.add(i);

                String dateLabel = "";
                try {
                    Date startDate = sdfInput.parse(firstInstallmentDateStr);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startDate);
                    if ("Monthly".equals(frequencyType)) {
                        cal.add(Calendar.MONTH, i - 1);
                    } else if ("Half Yearly".equals(frequencyType)) {
                        cal.add(Calendar.MONTH, (i - 1) * 6);
                    } else {
                        cal.add(Calendar.DATE, (i - 1) * 7);
                    }
                    dateLabel = "( " + sdfDialogOutput.format(cal.getTime()) + ") ";
                } catch (Exception ignored) {}

                filteredOptionsList.add(dateLabel + "Inst. " + i + " - ₹" + amt);
            }
        }

        if (openInstallmentNumbers.isEmpty()) {
            Toast.makeText(this, "All installments are already paid!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] optionsArray = filteredOptionsList.toArray(new String[0]);
        final boolean[] localCheckedTracker = new boolean[optionsArray.length];

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Select Pending Installments");
        builder.setMultiChoiceItems(optionsArray, localCheckedTracker, (dialog, which, isChecked) -> localCheckedTracker[which] = isChecked);
        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedInstallmentsList.clear();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < localCheckedTracker.length; i++) {
                if (localCheckedTracker[i]) {
                    int realNum = openInstallmentNumbers.get(i);
                    selectedInstallmentsList.add(realNum);
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(realNum);
                }
            }
            btnSelectInstallments.setText(selectedInstallmentsList.isEmpty() ? "Tap to Select Installments" : "Selected Inst: " + sb.toString());
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
    }

    private void resetInstallmentSelection() {
        selectedInstallmentsList.clear();
        btnSelectInstallments.setText("Tap to Select Installments");
    }

    private void refreshFundMatrixTable() {
        for (android.animation.ValueAnimator existingAnim : activeSnakeAnimators) existingAnim.cancel();
        activeSnakeAnimators.clear();
        tlFundTable.removeAllViews();
        if (chitId == null) return;

        android.graphics.drawable.GradientDrawable gridLine = new android.graphics.drawable.GradientDrawable();
        gridLine.setColor(Color.parseColor("#CBD5E1")); gridLine.setSize(2, 2); 
        tlFundTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
        tlFundTable.setDividerDrawable(gridLine);

        ArrayList<String> calculatedDatesHeaders = new ArrayList<>();
        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfOutput = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        int currentActiveIndexId = 0;
        Calendar todayCal = Calendar.getInstance();

        try {
            Date startDate = sdfInput.parse(firstInstallmentDateStr);
            Calendar cal = Calendar.getInstance();
            
            int elapsedIndex = -1;
            for (int i = 0; i < totalInstallmentsCount; i++) {
                cal.setTime(startDate);
                if ("Monthly".equals(frequencyType)) {
                    cal.add(Calendar.MONTH, i);
                } else if ("Half Yearly".equals(frequencyType)) {
                    cal.add(Calendar.MONTH, i * 6);
                } else {
                    cal.add(Calendar.DATE, i * 7);
                }
                calculatedDatesHeaders.add(sdfOutput.format(cal.getTime()));
                
                if ("Weekly".equals(frequencyType)) {
                    if (cal.getTimeInMillis() <= todayCal.getTimeInMillis() || 
                       (cal.get(Calendar.WEEK_OF_YEAR) == todayCal.get(Calendar.WEEK_OF_YEAR) && cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR))) {
                        elapsedIndex = i;
                    }
                } else {
                    int cY = cal.get(Calendar.YEAR);
                    int tY = todayCal.get(Calendar.YEAR);
                    int cM = cal.get(Calendar.MONTH);
                    int tM = todayCal.get(Calendar.MONTH);
                    
                    if (cY < tY || (cY == tY && cM <= tM)) {
                        elapsedIndex = i;
                    }
                }
            }
            currentActiveIndexId = (elapsedIndex != -1) ? elapsedIndex : 0;
        } catch (Exception ignored) {}

        if (!isMatrixVertical) {
            TableRow headerRow = new TableRow(this);
            headerRow.setBackgroundResource(R.drawable.table_header_bg);
            headerRow.setPadding(6, 12, 6, 12);

            TextView hNo = new TextView(this); hNo.setText("No."); hNo.setPadding(20, 16, 20, 16); hNo.setTextColor(Color.WHITE); hNo.setTypeface(null, Typeface.BOLD); headerRow.addView(hNo);
            TextView hName = new TextView(this); hName.setText("Member Name"); hName.setPadding(20, 16, 20, 16); hName.setTextColor(Color.WHITE); hName.setTypeface(null, Typeface.BOLD); hName.setGravity(Gravity.CENTER); headerRow.addView(hName);

            for (String dateStr : calculatedDatesHeaders) {
                TextView hDate = new TextView(this); hDate.setText(dateStr); hDate.setPadding(24, 16, 24, 16); hDate.setTextColor(Color.WHITE); hDate.setTypeface(null, Typeface.BOLD); headerRow.addView(hDate);
            }
            tlFundTable.addView(headerRow);

            int serialCounter = 1;
            for (String name : globalMembersList) {
                TableRow memberRow = new TableRow(this);
                memberRow.setPadding(6, 8, 6, 8);

                TextView tvSerial = new TextView(this); tvSerial.setText(String.valueOf(serialCounter++)); tvSerial.setPadding(20, 16, 20, 16); tvSerial.setTextColor(Color.parseColor("#64748B")); memberRow.addView(tvSerial);
                TextView tvName = new TextView(this); tvName.setText(name); tvName.setPadding(20, 16, 20, 16); tvName.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvName.setTextColor(Color.parseColor("#1E293B")); tvName.setGravity(Gravity.CENTER); memberRow.addView(tvName);

                for (int i = 1; i <= totalInstallmentsCount; i++) {
                    LinearLayout cellContainer = new LinearLayout(this); cellContainer.setPadding(12, 8, 12, 8); cellContainer.setGravity(Gravity.CENTER);
                    TextView tvStatusCell = new TextView(this); tvStatusCell.setTextSize(13); tvStatusCell.setPadding(16, 6, 16, 6); tvStatusCell.setTypeface(null, Typeface.BOLD);
                    
                    boolean isPaid = globalPaymentsCache.contains(chitId + "_" + name + "_" + i);
                    if (isPaid) {
                        tvStatusCell.setText(" Paid ✅ "); tvStatusCell.setTextColor(Color.parseColor("#047857")); tvStatusCell.setBackgroundResource(R.drawable.badge_paid_bg);
                    } else {
                        tvStatusCell.setText(" Pending "); tvStatusCell.setTextColor(Color.parseColor("#475569")); tvStatusCell.setBackgroundResource(R.drawable.badge_unpaid_bg);
                    }

                    if ((i - 1) == currentActiveIndexId) {
                        final SnakeBorderDrawable snakeDrawable = new SnakeBorderDrawable(Color.parseColor("#10B981"), isPaid ? Color.parseColor("#E6F4EA") : Color.parseColor("#F1F5F9"), 32f);
                        cellContainer.setBackground(snakeDrawable);
                        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0f, 1f); anim.setDuration(1600); anim.setRepeatCount(android.animation.ValueAnimator.INFINITE); anim.setInterpolator(new android.view.animation.LinearInterpolator());
                        anim.addUpdateListener(animation -> snakeDrawable.setAnimationProgress(-(float) animation.getAnimatedValue()));
                        anim.start(); activeSnakeAnimators.add(anim);
                    }
                    cellContainer.addView(tvStatusCell); memberRow.addView(cellContainer);
                }
                tlFundTable.addView(memberRow);
            }
        } else {
            TableRow headerRow = new TableRow(this);
            headerRow.setBackgroundResource(R.drawable.table_header_bg);
            headerRow.setPadding(6, 12, 6, 12);

            TextView hInst = new TextView(this); hInst.setText("Inst."); hInst.setPadding(20, 16, 20, 16); hInst.setTextSize(14); hInst.setTypeface(null, Typeface.BOLD); hInst.setTextColor(Color.WHITE); hInst.setGravity(Gravity.CENTER); headerRow.addView(hInst);
            TextView hDate = new TextView(this); hDate.setText("Due Date"); hDate.setPadding(20, 16, 20, 16); hDate.setTextSize(14); hDate.setTypeface(null, Typeface.BOLD); hDate.setTextColor(Color.WHITE); hDate.setGravity(Gravity.CENTER); headerRow.addView(hDate);

            for (String name : globalMembersList) {
                TextView hMemCol = new TextView(this); hMemCol.setText(name); hMemCol.setPadding(20, 16, 20, 16); hMemCol.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); hMemCol.setTextColor(Color.WHITE); hMemCol.setGravity(Gravity.CENTER); headerRow.addView(hMemCol);
            }
            tlFundTable.addView(headerRow);

            for (int i = 1; i <= totalInstallmentsCount; i++) {
                TableRow instRow = new TableRow(this);
                instRow.setPadding(6, 8, 6, 8);

                TextView tvInstNum = new TextView(this); tvInstNum.setText("#" + i); tvInstNum.setPadding(20, 16, 20, 16); tvInstNum.setTypeface(null, Typeface.BOLD); tvInstNum.setGravity(Gravity.CENTER); instRow.addView(tvInstNum);
                TextView tvInstDate = new TextView(this); tvInstDate.setText(calculatedDatesHeaders.get(i - 1)); tvInstDate.setPadding(20, 16, 20, 16); tvInstDate.setTextColor(Color.parseColor("#475569")); tvInstDate.setGravity(Gravity.CENTER); instRow.addView(tvInstDate);

                for (String name : globalMembersList) {
                    LinearLayout cellContainer = new LinearLayout(this); cellContainer.setPadding(12, 8, 12, 8); cellContainer.setGravity(Gravity.CENTER);
                    TextView tvStatusCell = new TextView(this); tvStatusCell.setTextSize(13); tvStatusCell.setPadding(16, 6, 16, 6); tvStatusCell.setTypeface(null, Typeface.BOLD);
                    
                    boolean isPaid = globalPaymentsCache.contains(chitId + "_" + name + "_" + i);
                    if (isPaid) {
                        tvStatusCell.setText(" Paid ✅ "); tvStatusCell.setTextColor(Color.parseColor("#047857")); tvStatusCell.setBackgroundResource(R.drawable.badge_paid_bg);
                    } else {
                        tvStatusCell.setText(" Pending "); tvStatusCell.setTextColor(Color.parseColor("#475569")); tvStatusCell.setBackgroundResource(R.drawable.badge_unpaid_bg);
                    }

                    if ((i - 1) == currentActiveIndexId) {
                        final SnakeBorderDrawable snakeDrawable = new SnakeBorderDrawable(Color.parseColor("#10B981"), isPaid ? Color.parseColor("#E6F4EA") : Color.parseColor("#F1F5F9"), 32f);
                        cellContainer.setBackground(snakeDrawable);
                        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0f, 1f); anim.setDuration(1600); anim.setRepeatCount(android.animation.ValueAnimator.INFINITE); anim.setInterpolator(new android.view.animation.LinearInterpolator());
                        anim.addUpdateListener(animation -> snakeDrawable.setAnimationProgress(-(float) animation.getAnimatedValue()));
                        anim.start(); activeSnakeAnimators.add(anim);
                    }
                    cellContainer.addView(tvStatusCell); instRow.addView(cellContainer);
                }
                tlFundTable.addView(instRow);
            }
        }
    }

    private void refreshAdvancesTable() {
        tlAdvancesTable.removeAllViews();
        TableRow headRow = new TableRow(this);
        headRow.setBackgroundResource(R.drawable.table_header_bg);
        headRow.setPadding(6, 12, 6, 12);

        android.graphics.drawable.GradientDrawable advancesDivider = new android.graphics.drawable.GradientDrawable();
        advancesDivider.setColor(Color.parseColor("#CBD5E1")); advancesDivider.setSize(2, 2);
        tlAdvancesTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
        tlAdvancesTable.setDividerDrawable(advancesDivider);

        String[] headers = {"Date Locked", "Chit Group", "Member Name", "Inst. #", "Advance Paid Out", "New Rate"};
        for (String h : headers) {
            TextView tv = new TextView(this); tv.setText(h); tv.setPadding(20, 16, 20, 16); tv.setTextColor(Color.WHITE); tv.setTypeface(null, Typeface.BOLD); 
            if (h.equals("Member Name") || h.equals("Advance Paid Out")) tv.setGravity(Gravity.CENTER);
            headRow.addView(tv);
        }
        tlAdvancesTable.addView(headRow);

        firestore.collection("advances").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            if (value == null) return;
            tlAdvancesTable.removeAllViews();
            tlAdvancesTable.addView(headRow);

            for (QueryDocumentSnapshot doc : value) {
                TableRow tr = new TableRow(this);
                tr.setPadding(6, 8, 6, 8);

                String cId = doc.getString("chitId");
                String cName = "Unknown Group";
                for (CloudChitItem item : globalChitsList) { if (item.id.equals(cId)) cName = item.name; }

                TextView tvDate = new TextView(this); tvDate.setText(doc.getString("date")); tvDate.setPadding(20, 16, 20, 16); tvDate.setTextColor(Color.parseColor("#475569")); tr.addView(tvDate);
                TextView tvChit = new TextView(this); tvChit.setText(cName); tvChit.setPadding(20, 16, 20, 16); tvChit.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvChit.setTextColor(Color.parseColor("#1E293B")); tr.addView(tvChit);
                TextView tvMem = new TextView(this); tvMem.setText(doc.getString("member_name")); tvMem.setPadding(20, 16, 20, 16); tvMem.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvMem.setTextColor(Color.parseColor("#1E293B")); tvMem.setGravity(Gravity.CENTER); tr.addView(tvMem);
                TextView tvInst = new TextView(this); tvInst.setText("Inst. " + doc.getLong("installment_num")); tvInst.setPadding(20, 16, 20, 16); tvInst.setTextColor(Color.parseColor("#475569")); tr.addView(tvInst);
                
                TextView tvAdv = new TextView(this); 
                tvAdv.setText("₹" + doc.getDouble("advance_amount")); 
                tvAdv.setPadding(20, 16, 20, 16); 
                tvAdv.setTypeface(null, Typeface.BOLD); 
                tvAdv.setTextColor(Color.parseColor("#E11D48")); 
                tvAdv.setGravity(Gravity.CENTER); 
                tr.addView(tvAdv);
                
                TextView tvRate = new TextView(this); tvRate.setText("₹" + doc.getDouble("new_amount")); tvRate.setPadding(20, 16, 20, 16); tvRate.setTypeface(null, Typeface.BOLD); tvRate.setTextColor(Color.parseColor("#047857")); tvRate.setGravity(Gravity.CENTER); tr.addView(tvRate);

                tlAdvancesTable.addView(tr);
            }
        });
    }

    private void refreshTransactionHistory() {
        tlHistoryTable.removeAllViews();
        TableRow headRow = new TableRow(this);
        headRow.setBackgroundResource(R.drawable.table_header_bg);
        headRow.setPadding(6, 12, 6, 12);

        android.graphics.drawable.GradientDrawable ledgerDivider = new android.graphics.drawable.GradientDrawable();
        ledgerDivider.setColor(Color.parseColor("#CBD5E1")); ledgerDivider.setSize(2, 2);
        tlHistoryTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
        tlHistoryTable.setDividerDrawable(ledgerDivider);

        String[] headers = {"Date", "Chit Group", "Member Name", "Inst.", "Amount Paid"};
        for (String h : headers) {
            TextView tv = new TextView(this); tv.setText(h); tv.setPadding(20, 16, 20, 16); tv.setTextColor(Color.WHITE); tv.setTypeface(null, Typeface.BOLD); 
            if (h.equals("Member Name") || h.equals("Amount Paid")) tv.setGravity(Gravity.CENTER);
            headRow.addView(tv);
        }
        tlHistoryTable.addView(headRow);

        firestore.collection("payments").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            if (value == null) return;
            tlHistoryTable.removeAllViews();
            tlHistoryTable.addView(headRow);

            double runningCashTotal = 0;
            int transactionEntriesCount = 0;

            for (QueryDocumentSnapshot doc : value) {
                String cId = doc.getString("chitId");
                if (!"ALL".equals(historyFilterChitId) && !historyFilterChitId.equals(cId)) continue;

                double amountPaid = doc.getDouble("amount");
                runningCashTotal += amountPaid;
                transactionEntriesCount++;

                TableRow tr = new TableRow(this);
                tr.setPadding(6, 8, 6, 8);

                TextView tvDate = new TextView(this); tvDate.setText(doc.getString("date")); tvDate.setPadding(20, 16, 20, 16); tvDate.setTextColor(Color.parseColor("#475569")); tr.addView(tvDate);
                String cName = "Unknown Group";
                for (CloudChitItem item : globalChitsList) { if (item.id.equals(cId)) cName = item.name; }

                TextView tvChit = new TextView(this); tvChit.setText(cName); tvChit.setPadding(20, 16, 20, 16); tvChit.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvChit.setTextColor(Color.parseColor("#1E293B")); tr.addView(tvChit);
                TextView tvMem = new TextView(this); tvMem.setText(doc.getString("member_name")); tvMem.setPadding(20, 16, 20, 16); tvMem.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvMem.setTextColor(Color.parseColor("#1E293B")); tvMem.setGravity(Gravity.CENTER); tr.addView(tvMem);
                
                LinearLayout badgeWrapper = new LinearLayout(this); badgeWrapper.setPadding(10, 6, 10, 6); badgeWrapper.setGravity(Gravity.CENTER);
                TextView tvInst = new TextView(this); tvInst.setText("Inst. " + doc.getLong("installment_num")); tvInst.setPadding(14, 4, 14, 4); tvInst.setTextColor(Color.parseColor("#475569")); tvInst.setBackgroundResource(R.drawable.badge_unpaid_bg);
                badgeWrapper.addView(tvInst); tr.addView(badgeWrapper);
                
                TextView tvAmt = new TextView(this); 
                tvAmt.setText("₹" + amountPaid); 
                tvAmt.setPadding(20, 16, 20, 16); 
                tvAmt.setTypeface(null, Typeface.BOLD); 
                tvAmt.setTextColor(Color.parseColor("#047857")); 
                tvAmt.setGravity(Gravity.CENTER); 
                tr.addView(tvAmt);

                tlHistoryTable.addView(tr);
            }
            tvHistorySummary.setText("Total Funds Collected: ₹" + runningCashTotal + "  |  Total Transactions: " + transactionEntriesCount);
        });
    }

    private void showLogAdvanceDialog() {
        if (chitId == null) {
            Toast.makeText(this, "Please create/select a Chit Group first.", Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_log_advance, null);

        final AutoCompleteTextView acMem = view.findViewById(R.id.acMem);
        final TextInputEditText etInst = view.findViewById(R.id.etInstNum);
        final TextInputEditText etAdvanceAmt = view.findViewById(R.id.etAdvanceAmt);
        final TextInputEditText etAmt = view.findViewById(R.id.etNewAmt);

        acMem.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_member, globalMembersList));
        builder.setView(view);
        builder.setPositiveButton("Save Advance Rules", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog dialog = builder.create(); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String memName = acMem.getText().toString().trim();
            String instStr = etInst.getText().toString().trim();
            String advAmtStr = etAdvanceAmt.getText().toString().trim();
            String amtStr = etAmt.getText().toString().trim();

            if (memName.isEmpty() || instStr.isEmpty() || advAmtStr.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill out all fields completely.", Toast.LENGTH_SHORT).show();
                return;
            }

            int instNum = Integer.parseInt(instStr);
            double advAmt = Double.parseDouble(advAmtStr);
            double newAmt = Double.parseDouble(amtStr);

            if (instNum < 1 || instNum > totalInstallmentsCount) {
                Toast.makeText(MainActivity.this, "Invalid installment milestone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Map<String, Object> advancePayload = new HashMap<>();
            advancePayload.put("chitId", chitId); advancePayload.put("installment_num", instNum);
            advancePayload.put("member_name", memName); advancePayload.put("advance_amount", advAmt);
            advancePayload.put("new_amount", newAmt); advancePayload.put("date", currentDate);

            firestore.collection("advances").add(advancePayload).addOnSuccessListener(ref -> {
                Toast.makeText(MainActivity.this, "Advance configuration saved to Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
    }

    private void showNewChitDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
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

        spFrequency.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, new String[]{"Monthly", "Weekly", "Half Yearly"}));
        spAmountType.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, new String[]{"Fixed Amount", "Random Amount"}));

        TextInputLayout tlMemberWrap = new TextInputLayout(this); tlMemberWrap.setHint("Primary Member Name");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16); tlMemberWrap.setLayoutParams(lp);

        TextInputEditText etSingleMember = new TextInputEditText(this); tlMemberWrap.addView(etSingleMember);
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
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> etDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
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
                Toast.makeText(MainActivity.this, "Fill in all basic fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalInst = Integer.parseInt(instStr);
            ArrayList<Double> amountsArray = new ArrayList<>();

            if (amtType.equals("Fixed Amount")) {
                if (etAmount.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please specify an installment amount.", Toast.LENGTH_SHORT).show();
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

            firestore.collection("chits").add(chitPayload).addOnSuccessListener(docRef -> {
                String newId = docRef.getId();
                for (TextInputEditText field : dynamicMemberFields) {
                    String mName = field.getText().toString().trim();
                    if(!mName.isEmpty()){
                        Map<String, Object> mPayload = new HashMap<>();
                        mPayload.put("chitId", newId); mPayload.put("name", mName);
                        firestore.collection("members").add(mPayload);
                    }
                }
                Toast.makeText(MainActivity.this, "Chit Synchronized to Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss(); chitId = newId;
                syncCurrentChitContextFromCloud();
            });
        });
    }

    private void triggerDynamicAmountFields(String countStr, LinearLayout container, ArrayList<TextInputEditText> fieldTrackerList) {
        container.removeAllViews(); fieldTrackerList.clear();
        if (!countStr.trim().isEmpty()) {
            int total = Integer.parseInt(countStr.trim());
            for (int i = 1; i <= total; i++) {
                TextInputLayout wrap = new TextInputLayout(this); wrap.setHint("Installment " + i + " Amount (₹)");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12); wrap.setLayoutParams(lp);

                TextInputEditText etAmtInput = new TextInputEditText(this);
                etAmtInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                wrap.addView(etAmtInput); container.addView(wrap); fieldTrackerList.add(etAmtInput);
            }
        }
    }
}
