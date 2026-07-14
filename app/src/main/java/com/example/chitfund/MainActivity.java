package com.example.chitfund;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public FirebaseFirestore firestore;
    public String chitId = null; 
    private String historyFilterChitId = "ALL"; 

    private AutoCompleteTextView spChitSelector;
    public AutoCompleteTextView spMembers;
    private AutoCompleteTextView spHistoryFilter;
    public Button btnSelectInstallments;
    private Button btnToggleMatrixOrientation;
    public TableLayout tlFundTable;
    private TableLayout tlAdvancesTable;
    private TableLayout tlGlobalSummaryTable; 
    private LinearLayout llGlobalSummaryContainer; 
    
    private LinearLayout llRemindersContainer; 
    public LinearLayout globalNoteContainer;
    public android.os.Handler notesAnimationHandler = new android.os.Handler();
    public Runnable notesAnimationRunnable;
    public int currentGlobalNoteIndex = 0;
    public ArrayList<String> currentGlobalNotesList = new ArrayList<>();
    public android.animation.ValueAnimator noteCardAnimator = null;
    
    public TextView tvFundTitle;
    public View llFormContainer;
    private TextView tvHistorySummary;

    private androidx.recyclerview.widget.RecyclerView rvHistoryTable;
    private LedgerComponents.LedgerAdapter ledgerAdapter;
    private ArrayList<LedgerComponents.LedgerTransaction> currentLedgerData = new ArrayList<>();

    private View tabContainerMatrix;
    public View tabContainerCollect;
    private View tabContainerLedger;
    private View tabContainerAdvances;
    
    public int totalInstallmentsCount;
    public String frequencyType;
    public String firstInstallmentDateStr;

    private boolean isMatrixVertical = false;

    public HashMap<String, Double> globalPaymentsCache = new HashMap<>(); 
    public HashMap<String, ArrayList<String>> globalChitMembersCache = new HashMap<>(); 
    public HashMap<String, Integer> globalAdvanceStartCache = new HashMap<>(); 
    public HashMap<String, Double> globalAdvanceRateCache = new HashMap<>(); 
    public HashMap<String, String> globalAdvanceDateCache = new HashMap<>(); 
    
    public HashMap<String, Double> globalChitTotalAdvancesCache = new HashMap<>();
    public HashMap<String, ArrayList<Double>> globalChitAmountsCache = new HashMap<>();
    public HashMap<String, String> globalChitStartDatesCache = new HashMap<>();
    public HashMap<String, String> globalChitFrequenciesCache = new HashMap<>();
    public HashMap<String, Integer> globalChitInstallmentsCountCache = new HashMap<>();
    
    // RESTORED: Missing Variable
    public ArrayList<Double> baseChitInstallmentAmounts = new ArrayList<>(); 
    
    private ArrayList<android.animation.ValueAnimator> activeSnakeAnimators = new ArrayList<>();
    private android.animation.ValueAnimator globalSummaryAnimator = null; 
    public ArrayList<Integer> selectedInstallmentsList = new ArrayList<>();
    
    public ArrayList<LedgerComponents.CloudChitItem> globalChitsList = new ArrayList<>();
    public ArrayList<String> globalMembersList = new ArrayList<>();

    public DialogEngine dialogEngine;

    @Override
    protected void onCreate(Bundle Bundle) {
        super.onCreate(Bundle);
        setContentView(R.layout.activity_main);
        
        firestore = FirebaseFirestore.getInstance();
        dialogEngine = new DialogEngine(this);

        spChitSelector = findViewById(R.id.spChitSelector);
        spMembers = findViewById(R.id.spMembers);
        spHistoryFilter = findViewById(R.id.spHistoryFilter);
        
        spChitSelector.setInputType(android.text.InputType.TYPE_NULL);
        spHistoryFilter.setInputType(android.text.InputType.TYPE_NULL);

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

        rvHistoryTable = findViewById(R.id.rvHistoryTable);
        rvHistoryTable.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        ledgerAdapter = new LedgerComponents.LedgerAdapter();
        rvHistoryTable.setAdapter(ledgerAdapter);

        tabContainerMatrix = findViewById(R.id.tabContainerMatrix);
        tabContainerCollect = findViewById(R.id.tabContainerCollect);
        tabContainerLedger = findViewById(R.id.tabContainerLedger);
        tabContainerAdvances = findViewById(R.id.tabContainerAdvances);

        llRemindersContainer = new LinearLayout(this);
        llRemindersContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams remParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        remParams.setMargins(0, 0, 0, 40); 
        llRemindersContainer.setLayoutParams(remParams);

        ViewGroup parentGroup = (ViewGroup) llGlobalSummaryContainer.getParent();
        if(parentGroup != null) {
            int summaryIndex = parentGroup.indexOfChild(llGlobalSummaryContainer);
            parentGroup.addView(llRemindersContainer, summaryIndex);
        }

        float radiusPx = 24 * getResources().getDisplayMetrics().density;
        final PremiumUI.SnakeBorderDrawable globalSnakeDrawable = new PremiumUI.SnakeBorderDrawable(Color.parseColor("#F59E0B"), Color.parseColor("#FFF7ED"), radiusPx);
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
            LedgerComponents.CloudChitItem selected = (LedgerComponents.CloudChitItem) parent.getItemAtPosition(position);
            if (selected != null && !selected.id.equals(chitId)) {
                chitId = selected.id;
                syncCurrentChitContextFromCloud();
            }
        });

        spHistoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = parent.getItemAtPosition(position).toString();
            if (selectedName.equals("All Chits")) {
                historyFilterChitId = "ALL";
            } else {
                for (LedgerComponents.CloudChitItem item : globalChitsList) {
                    if (item.name.equals(selectedName)) {
                        historyFilterChitId = item.id;
                        break;
                    }
                }
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

        btnSelectInstallments.setOnClickListener(v -> dialogEngine.showMultiSelectInstallmentsDialog());
        btnAddInstallment.setOnClickListener(v -> dialogEngine.showConfirmPaymentDialog());

        initGlobalDatabaseSynchronizers();
        refreshGlobalNoteCard();
        refreshTransactionHistory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        menu.add(Menu.NONE, 1001, Menu.NONE, "Add Notes").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_new_chit) { dialogEngine.showNewChitDialog(); return true; }
        if (item.getItemId() == R.id.menu_log_advance) { dialogEngine.showLogAdvanceDialog(); return true; }
        if (item.getItemId() == R.id.menu_delete_chit) { showDeleteChitSelectionDialog(); return true; }
        if (item.getItemId() == 1001) { dialogEngine.showAddNotesDialog(); return true; }
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
                globalChitsList.add(new LedgerComponents.CloudChitItem(id, doc.getString("name")));
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
                            double amt = pDoc.getDouble("amount") != null ? pDoc.getDouble("amount") : 0.0;
                            
                            double currentSum = globalPaymentsCache.containsKey(compositeKey) ? globalPaymentsCache.get(compositeKey) : 0.0;
                            globalPaymentsCache.put(compositeKey, currentSum + amt);
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
        for (LedgerComponents.CloudChitItem item : globalChitsList) filterOptions.add(item.name);
        spHistoryFilter.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_premium, filterOptions));

        ArrayAdapter<LedgerComponents.CloudChitItem> adapter = new ArrayAdapter<>(this, R.layout.list_item_premium, globalChitsList);
        spChitSelector.setAdapter(adapter);

        if (!globalChitsList.isEmpty() && chitId == null) {
            spChitSelector.setText(globalChitsList.get(0).name, false);
            chitId = globalChitsList.get(0).id;
        }
    }

    private void calculateGlobalMonthlyDuesEngine() {
        tlGlobalSummaryTable.removeAllViews();
        llRemindersContainer.removeAllViews(); 
        if (globalChitsList.isEmpty()) return;

        java.util.Collections.sort(globalChitsList, (c1, c2) -> {
            String f1 = globalChitFrequenciesCache.containsKey(c1.id) ? globalChitFrequenciesCache.get(c1.id) : "";
            String f2 = globalChitFrequenciesCache.containsKey(c2.id) ? globalChitFrequenciesCache.get(c2.id) : "";
            
            int w1 = "Weekly".equals(f1) ? 1 : ("Monthly".equals(f1) ? 2 : ("Half Yearly".equals(f1) ? 3 : 4));
            int w2 = "Weekly".equals(f2) ? 1 : ("Monthly".equals(f2) ? 2 : ("Half Yearly".equals(f2) ? 3 : 4));
            
            return Integer.compare(w1, w2);
        });

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

        for (LedgerComponents.CloudChitItem item : globalChitsList) {
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

            boolean isUpcomingHalfYearly = false;
            int upcomingStepNumber = 0;
            double upcomingExpectedTotal = 0.0;

            double calcTotalPlanAmount = 0.0;
            double calcTotalPaidAmount = 0.0;
            int calcPaidInstCount = 0; 
            ArrayList<Double> dynamicPlanBreakdown = new ArrayList<>();
            ArrayList<Integer> pendingStepsList = new ArrayList<>();
            
            ArrayList<Integer> weeklyStepsThisMonth = new ArrayList<>();
            ArrayList<Integer> activeStepsList = new ArrayList<>(); 

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

                    int nextMonthY = tM == Calendar.DECEMBER ? tY + 1 : tY;
                    int nextMonthM = tM == Calendar.DECEMBER ? Calendar.JANUARY : tM + 1;

                    if (cY == tY && cM == tM) {
                        isCurrent = true;
                        hasMilestoneThisMonth = true;
                        if ("Weekly".equals(freq)) {
                            weeklyStepsThisMonth.add(step);
                        }
                    } else if (cY < tY || (cY == tY && cM < tM)) {
                        isPast = true;
                    } else if ("Half Yearly".equals(freq) && cY == nextMonthY && cM == nextMonthM) {
                        isUpcomingHalfYearly = true;
                        upcomingStepNumber = step;
                    }

                    if (isCurrent || isPast) {
                        highestPassedOrCurrentStep = step;
                    }
                    
                    if (isCurrent) {
                        activeStepsList.add(step);
                    }

                    double stepExpectedTotal = 0.0;
                    boolean stepIsPending = false;

                    for (String mName : members) {
                        double stepAmt = getSpecificCachedMemberInstallmentAmount(id, mName, step);
                        stepExpectedTotal += stepAmt;

                        String payKey = id + "_" + mName + "_" + step;
                        double paidAmt = globalPaymentsCache.containsKey(payKey) ? globalPaymentsCache.get(payKey) : 0.0;
                        
                        if (paidAmt < stepAmt) {
                            double pendingForThisStep = stepAmt - paidAmt;
                            if (isCurrent || isPast) stepIsPending = true;
                            if (isCurrent) {
                                currentMonthChitPending += pendingForThisStep;
                            } else if (isPast) {
                                previousArrearsChitPending += pendingForThisStep;
                            }
                            
                            if (isUpcomingHalfYearly && step == upcomingStepNumber) {
                                stepIsPending = true;
                                upcomingExpectedTotal += pendingForThisStep;
                            }
                        } 
                        
                        calcTotalPaidAmount += paidAmt;
                        if (paidAmt >= stepAmt && stepAmt > 0) calcPaidInstCount++; 
                    }
                    
                    if (isUpcomingHalfYearly && step == upcomingStepNumber && !stepIsPending) {
                        isUpcomingHalfYearly = false;
                        upcomingExpectedTotal = 0.0;
                    }
                    
                    dynamicPlanBreakdown.add(stepExpectedTotal);
                    calcTotalPlanAmount += stepExpectedTotal;
                    
                    if (stepIsPending) {
                        pendingStepsList.add(step);
                    }
                }
            } catch (Exception ignored) {}

            boolean isPureReminder = (!hasMilestoneThisMonth && previousArrearsChitPending == 0 && isUpcomingHalfYearly);

            if (isPureReminder) {
                LinearLayout reminderCard = new LinearLayout(this);
                reminderCard.setOrientation(LinearLayout.HORIZONTAL);
                reminderCard.setPadding(50, 40, 50, 50); 
                reminderCard.setGravity(Gravity.CENTER_VERTICAL);
                
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(Color.parseColor("#FEF3C7")); 
                bg.setCornerRadius(32f); 
                reminderCard.setBackground(bg);
                
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(50, 10, 50, 15); 
                reminderCard.setLayoutParams(cardParams);
                
                TextView icon = new TextView(this);
                icon.setText("⚠️");
                icon.setTextSize(16);
                icon.setPadding(0, 0, 20, 0);
                icon.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                reminderCard.addView(icon);
                
                TextView msg = new TextView(this);
                android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
                
                ssb.append("YOU HAVE A HALF-YEARLY INSTALLMENT OF ");
                
                int startIdx = ssb.length();
                ssb.append("₹").append(String.format(Locale.getDefault(), "%,.0f", upcomingExpectedTotal));
                ssb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), startIdx, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#15803D")), startIdx, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                ssb.append(" DUE NEXT MONTH FOR ");
                
                startIdx = ssb.length();
                ssb.append(item.name.toUpperCase(Locale.getDefault()));
                ssb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), startIdx, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#15803D")), startIdx, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                ssb.append(". (INSTALLMENT NO. ");
                
                startIdx = ssb.length();
                ssb.append("#").append(String.valueOf(upcomingStepNumber));
                ssb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), startIdx, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(")");
                
                msg.setText(ssb);
                msg.setTextColor(Color.parseColor("#B45309")); 
                
                msg.setTextSize(12f); 
                msg.setTypeface(Typeface.MONOSPACE);
                
                msg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                
                reminderCard.addView(msg);
                
                llRemindersContainer.addView(reminderCard);
                continue; 
            }

            if (!hasMilestoneThisMonth && previousArrearsChitPending == 0) {
                continue; 
            }

            double totalChitOutstanding = currentMonthChitPending + previousArrearsChitPending;
            
            if (!isPureReminder) {
                aggregateCurrentPending += currentMonthChitPending;
                aggregatePreviousPending += previousArrearsChitPending;
            }

            android.text.SpannableStringBuilder instSpannable = new android.text.SpannableStringBuilder();

            if ("Weekly".equals(freq) && !weeklyStepsThisMonth.isEmpty()) {
                for(int i=0; i < weeklyStepsThisMonth.size(); i++) {
                    int currentStep = weeklyStepsThisMonth.get(i);
                    String stepStr = "#" + String.valueOf(currentStep);
                    
                    int startIdx = instSpannable.length();
                    instSpannable.append(stepStr);
                    int endIdx = instSpannable.length();
                    
                    if (!pendingStepsList.contains(currentStep)) {
                        instSpannable.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#15803D")), startIdx, endIdx, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        instSpannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), startIdx, endIdx, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    
                    if(i < weeklyStepsThisMonth.size() - 1) {
                        instSpannable.append(", ");
                    }
                }
            } else if (!isPureReminder) {
                int displayInstNumber = hasMilestoneThisMonth ? highestPassedOrCurrentStep : Math.min(highestPassedOrCurrentStep + 1, maxInst);
                instSpannable.append("#").append(String.valueOf(displayInstNumber));
                
                boolean isStepActiveOrPast = displayInstNumber <= highestPassedOrCurrentStep;
                if (!pendingStepsList.contains(displayInstNumber) && isStepActiveOrPast) {
                    instSpannable.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#15803D")), 0, instSpannable.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    instSpannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, instSpannable.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (hasMilestoneThisMonth && activeStepsList.isEmpty()) {
                    activeStepsList.add(displayInstNumber);
                }
            }

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
            final ArrayList<Integer> targetActiveSteps = activeStepsList; 
            
            final String targetActiveInstStr = instSpannable.toString();

            tvName.setOnClickListener(v -> {
                PremiumUI.showPremiumChitSummaryDialog(MainActivity.this, 
                    targetName, targetStartDate, targetFreq, targetMaxInst, targetActiveInstStr, 
                    targetMembers, curMonthDues, pastMonthDues, grossDues, totalAdvancesTaken, 
                    targetPlanBreakdownList, targetPendingSteps, targetPlanAmount, targetPaidAmount, 
                    targetBalance, targetAdvanceLogs, targetPaidInstCount, targetRemainingInstCount, targetActiveSteps
                );
            });

            row.addView(tvName);
            
            TextView tvInst = new TextView(this); 
            tvInst.setText(instSpannable); 
            tvInst.setPadding(20, 12, 20, 12); 
            tvInst.setGravity(Gravity.CENTER); 
            tvInst.setTextColor(Color.parseColor("#475569")); 
            tvInst.setTypeface(Typeface.MONOSPACE); 
            row.addView(tvInst);
            
            TextView tvCur = new TextView(this); tvCur.setText("₹" + String.format(Locale.getDefault(), "%,.1f", currentMonthChitPending)); tvCur.setPadding(20, 12, 20, 12); tvCur.setGravity(Gravity.CENTER); tvCur.setTextColor(Color.parseColor("#1E293B")); row.addView(tvCur);
            TextView tvPrev = new TextView(this); tvPrev.setText("₹" + String.format(Locale.getDefault(), "%,.1f", previousArrearsChitPending)); tvPrev.setPadding(20, 12, 20, 12); tvPrev.setGravity(Gravity.CENTER); tvPrev.setTextColor(previousArrearsChitPending > 0 ? Color.parseColor("#DC2626") : Color.parseColor("#64748B")); if(previousArrearsChitPending > 0) tvPrev.setTypeface(null, Typeface.BOLD); row.addView(tvPrev);
            TextView tvTot = new TextView(this); tvTot.setText("₹" + String.format(Locale.getDefault(), "%,.1f", totalChitOutstanding)); tvTot.setPadding(20, 12, 20, 12); tvTot.setGravity(Gravity.CENTER); tvTot.setTextColor(Color.parseColor("#0F172A")); tvTot.setTypeface(null, Typeface.BOLD); row.addView(tvTot); 

            tlGlobalSummaryTable.addView(row);
        }

        TableRow footerRow = new TableRow(this);
        footerRow.setBackgroundResource(R.drawable.table_footer_bg); 
        footerRow.setPadding(4, 12, 4, 12);

        TextView tvTotalLbl = new TextView(this); tvTotalLbl.setText("GRAND TOTALS"); tvTotalLbl.setPadding(20, 12, 20, 12); tvTotalLbl.setTextColor(Color.parseColor("#0F172A")); tvTotalLbl.setTypeface(null, Typeface.BOLD); footerRow.addView(tvTotalLbl);
        TextView tvEmpty = new TextView(this); tvEmpty.setText("-"); tvEmpty.setPadding(20, 12, 20, 12); tvEmpty.setGravity(Gravity.CENTER); tvEmpty.setTextColor(Color.TRANSPARENT); footerRow.addView(tvEmpty);
        TextView tvSumCur = new TextView(this); tvSumCur.setText("₹" + String.format(Locale.getDefault(), "%,.1f", aggregateCurrentPending)); tvSumCur.setPadding(20, 12, 20, 12); tvSumCur.setGravity(Gravity.CENTER); tvSumCur.setTextColor(Color.parseColor("#15803D")); tvSumCur.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumCur);
        TextView tvSumPrev = new TextView(this); tvSumPrev.setText("₹" + String.format(Locale.getDefault(), "%,.1f", aggregatePreviousPending)); tvSumPrev.setPadding(20, 12, 20, 12); tvSumPrev.setGravity(Gravity.CENTER); tvSumPrev.setTextColor(Color.parseColor("#B91C1C")); tvSumPrev.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumPrev);
        TextView tvSumGrand = new TextView(this); tvSumGrand.setText("₹" + String.format(Locale.getDefault(), "%,.1f", (aggregateCurrentPending + aggregatePreviousPending))); tvSumGrand.setPadding(20, 12, 20, 12); tvSumGrand.setGravity(Gravity.CENTER); tvSumGrand.setTextColor(Color.parseColor("#0F172A")); tvSumGrand.setTypeface(null, Typeface.BOLD); footerRow.addView(tvSumGrand);

        tlGlobalSummaryTable.addView(footerRow);
    }

    public double getSpecificCachedMemberInstallmentAmount(String targetChitId, String memberName, int installmentNum) {
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

    public void syncCurrentChitContextFromCloud() {
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

    public void resetInstallmentSelection() {
        selectedInstallmentsList.clear();
        btnSelectInstallments.setText("Tap to Select Installments");
    }

    public void refreshTransactionHistory() {
        ViewGroup parentGroup = findViewById(R.id.llLedgerTableWrapper);
        
        if (parentGroup != null && parentGroup.findViewById(9999) == null) {
            LinearLayout headRow = new LinearLayout(this);
            headRow.setId(9999);
            headRow.setBackgroundResource(R.drawable.table_header_bg);
            headRow.setPadding(6, 12, 6, 12);
            headRow.setOrientation(LinearLayout.HORIZONTAL);

            String[] headers = {"Date", "Chit Group", "Member Name", "Inst.", "Amount Paid"};
            
            float density = getResources().getDisplayMetrics().density;
            int[] widthsDp = {100, 140, 140, 80, 120}; 

            for (int i=0; i<headers.length; i++) {
                TextView tv = new TextView(this); 
                tv.setText(headers[i]); 
                tv.setPadding(10, 16, 10, 16); 
                tv.setTextColor(Color.WHITE); 
                tv.setTypeface(null, Typeface.BOLD); 
                tv.setGravity(Gravity.CENTER);
                
                int widthPx = (int) (widthsDp[i] * density);
                LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
                tv.setLayoutParams(hLp);
                headRow.addView(tv);
            }
            parentGroup.addView(headRow, 0);
        }

        firestore.collection("payments").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            if (value == null) return;

            double runningCashTotal = 0;
            int transactionEntriesCount = 0;
            ArrayList<LedgerComponents.LedgerTransaction> newData = new ArrayList<>();

            for (QueryDocumentSnapshot doc : value) {
                String cId = doc.getString("chitId");
                if (!"ALL".equals(historyFilterChitId) && !historyFilterChitId.equals(cId)) continue;
                
                String rawMemName = doc.getString("member_name");
                double amountPaid = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0.0;
                runningCashTotal += amountPaid;
                transactionEntriesCount++;

                String date = doc.getString("date");
                String cName = "Unknown Group";
                for (LedgerComponents.CloudChitItem item : globalChitsList) { if (item.id.equals(cId)) cName = item.name; }
                String notes = doc.getString("notes");
                long instNum = doc.getLong("installment_num") != null ? doc.getLong("installment_num") : 0;

                newData.add(new LedgerComponents.LedgerTransaction(date, cName, rawMemName, notes, instNum, amountPaid));
            }

            currentLedgerData = newData;
            ledgerAdapter.updateData(currentLedgerData);
            tvHistorySummary.setText("Total Funds Collected: ₹" + String.format(Locale.getDefault(), "%,.1f", runningCashTotal) + "  |  Total Transactions: " + transactionEntriesCount);
        });
    }

    public void refreshFundMatrixTable() {
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
        
        ArrayList<Integer> currentActiveIndices = new ArrayList<>();
        int elapsedIndex = -1;
        Calendar todayCal = Calendar.getInstance();

        try {
            Date startDate = sdfInput.parse(firstInstallmentDateStr);
            Calendar cal = Calendar.getInstance();
            
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
                
                boolean isCurrent = false;
                int cY = cal.get(Calendar.YEAR);
                int tY = todayCal.get(Calendar.YEAR);
                int cM = cal.get(Calendar.MONTH);
                int tM = todayCal.get(Calendar.MONTH);

                if ("Weekly".equals(frequencyType)) {
                    int cW = cal.get(Calendar.WEEK_OF_YEAR);
                    int tW = todayCal.get(Calendar.WEEK_OF_YEAR);
                    if (cW == tW && cY == tY) { isCurrent = true; }
                    if (cal.getTimeInMillis() <= todayCal.getTimeInMillis() || isCurrent) { elapsedIndex = i; }
                } else {
                    if (cY == tY && cM == tM) { isCurrent = true; }
                    if (cY < tY || (cY == tY && cM <= tM)) { elapsedIndex = i; }
                }
                
                if (isCurrent) currentActiveIndices.add(i);
            }
        } catch (Exception ignored) {}

        ArrayList<Integer> snakeIndices = new ArrayList<>(currentActiveIndices);
        if (snakeIndices.isEmpty() && elapsedIndex != -1) {
            snakeIndices.add(elapsedIndex);
        } else if (snakeIndices.isEmpty()) {
            snakeIndices.add(0);
        }

        if (!isMatrixVertical) {
            TableRow headerRow = new TableRow(this);
            headerRow.setBackgroundResource(R.drawable.table_header_bg);
            headerRow.setPadding(6, 12, 6, 12);

            TextView hNo = new TextView(this); hNo.setText("No."); hNo.setPadding(20, 16, 20, 16); hNo.setTextColor(Color.WHITE); hNo.setTypeface(null, Typeface.BOLD); headerRow.addView(hNo);
            TextView hName = new TextView(this); hName.setText("Member Name"); hName.setPadding(20, 16, 20, 16); hName.setTextColor(Color.WHITE); hName.setTypeface(null, Typeface.BOLD); hName.setGravity(Gravity.CENTER); headerRow.addView(hName);

            int dateIdx = 0;
            for (String dateStr : calculatedDatesHeaders) {
                TextView hDate = new TextView(this); 
                hDate.setText(dateStr); 
                hDate.setPadding(24, 16, 24, 16); 
                
                if (currentActiveIndices.contains(dateIdx)) {
                    hDate.setTextColor(Color.parseColor("#34D399"));
                } else {
                    hDate.setTextColor(Color.WHITE);
                }
                hDate.setTypeface(null, Typeface.BOLD); 
                headerRow.addView(hDate);
                dateIdx++;
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
                    
                    double expectedAmt = getSpecificCachedMemberInstallmentAmount(chitId, name, i);
                    String compositeKey = chitId + "_" + name + "_" + i;
                    double paidAmt = globalPaymentsCache.containsKey(compositeKey) ? globalPaymentsCache.get(compositeKey) : 0.0;
                    
                    boolean isFullyPaid = (paidAmt >= expectedAmt && expectedAmt > 0);

                    if (isFullyPaid) {
                        tvStatusCell.setText(" Paid ✅ "); tvStatusCell.setTextColor(Color.parseColor("#047857")); tvStatusCell.setBackgroundResource(R.drawable.badge_paid_bg);
                    } else if (paidAmt > 0) {
                        tvStatusCell.setText(" Part ⏳ "); tvStatusCell.setTextColor(Color.parseColor("#B45309")); 
                        android.graphics.drawable.GradientDrawable partBg = new android.graphics.drawable.GradientDrawable();
                        partBg.setColor(Color.parseColor("#FEF3C7")); partBg.setCornerRadius(16f);
                        tvStatusCell.setBackground(partBg);
                    } else {
                        tvStatusCell.setText(" Pending "); tvStatusCell.setTextColor(Color.parseColor("#475569")); tvStatusCell.setBackgroundResource(R.drawable.badge_unpaid_bg);
                    }

                    if (snakeIndices.contains(i - 1)) {
                        final PremiumUI.SnakeBorderDrawable snakeDrawable = new PremiumUI.SnakeBorderDrawable(Color.parseColor("#10B981"), isFullyPaid ? Color.parseColor("#E6F4EA") : Color.parseColor("#F1F5F9"), 32f);
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
                int colIdx = i - 1;
                TableRow instRow = new TableRow(this);
                instRow.setPadding(6, 8, 6, 8);

                TextView tvInstNum = new TextView(this); 
                tvInstNum.setText("#" + i); 
                tvInstNum.setPadding(20, 16, 20, 16); 
                tvInstNum.setGravity(Gravity.CENTER);
                
                TextView tvInstDate = new TextView(this); 
                tvInstDate.setText(calculatedDatesHeaders.get(colIdx)); 
                tvInstDate.setPadding(20, 16, 20, 16); 
                tvInstDate.setGravity(Gravity.CENTER); 
                
                if (currentActiveIndices.contains(colIdx)) {
                    tvInstNum.setTextColor(Color.parseColor("#15803D"));
                    tvInstNum.setTypeface(null, Typeface.BOLD);
                    
                    tvInstDate.setTextColor(Color.parseColor("#15803D"));
                    tvInstDate.setTypeface(null, Typeface.BOLD);
                } else {
                    tvInstNum.setTextColor(Color.parseColor("#0F172A"));
                    tvInstNum.setTypeface(null, Typeface.BOLD);
                    
                    tvInstDate.setTextColor(Color.parseColor("#475569"));
                    tvInstDate.setTypeface(null, Typeface.NORMAL);
                }
                
                instRow.addView(tvInstNum);
                instRow.addView(tvInstDate);

                for (String name : globalMembersList) {
                    LinearLayout cellContainer = new LinearLayout(this); cellContainer.setPadding(12, 8, 12, 8); cellContainer.setGravity(Gravity.CENTER);
                    TextView tvStatusCell = new TextView(this); tvStatusCell.setTextSize(13); tvStatusCell.setPadding(16, 6, 16, 6); tvStatusCell.setTypeface(null, Typeface.BOLD);
                    
                    double expectedAmt = getSpecificCachedMemberInstallmentAmount(chitId, name, i);
                    String compositeKey = chitId + "_" + name + "_" + i;
                    double paidAmt = globalPaymentsCache.containsKey(compositeKey) ? globalPaymentsCache.get(compositeKey) : 0.0;
                    
                    boolean isFullyPaid = (paidAmt >= expectedAmt && expectedAmt > 0);

                    if (isFullyPaid) {
                        tvStatusCell.setText(" Paid ✅ "); tvStatusCell.setTextColor(Color.parseColor("#047857")); tvStatusCell.setBackgroundResource(R.drawable.badge_paid_bg);
                    } else if (paidAmt > 0) {
                        tvStatusCell.setText(" Part ⏳ "); tvStatusCell.setTextColor(Color.parseColor("#B45309")); 
                        android.graphics.drawable.GradientDrawable partBg = new android.graphics.drawable.GradientDrawable();
                        partBg.setColor(Color.parseColor("#FEF3C7")); partBg.setCornerRadius(16f);
                        tvStatusCell.setBackground(partBg);
                    } else {
                        tvStatusCell.setText(" Pending "); tvStatusCell.setTextColor(Color.parseColor("#475569")); tvStatusCell.setBackgroundResource(R.drawable.badge_unpaid_bg);
                    }

                    if (snakeIndices.contains(colIdx)) {
                        final PremiumUI.SnakeBorderDrawable snakeDrawable = new PremiumUI.SnakeBorderDrawable(Color.parseColor("#10B981"), isFullyPaid ? Color.parseColor("#E6F4EA") : Color.parseColor("#F1F5F9"), 32f);
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

    public void refreshAdvancesTable() {
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
                for (LedgerComponents.CloudChitItem item : globalChitsList) { if (item.id.equals(cId)) cName = item.name; }

                TextView tvDate = new TextView(this); tvDate.setText(doc.getString("date")); tvDate.setPadding(20, 16, 20, 16); tvDate.setTextColor(Color.parseColor("#475569")); tr.addView(tvDate);
                TextView tvChit = new TextView(this); tvChit.setText(cName); tvChit.setPadding(20, 16, 20, 16); tvChit.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); tvChit.setTextColor(Color.parseColor("#1E293B")); tr.addView(tvChit);
                
                LinearLayout memLayout = new LinearLayout(this);
                memLayout.setOrientation(LinearLayout.VERTICAL);
                memLayout.setGravity(Gravity.CENTER);
                
                TextView tvMem = new TextView(this); 
                tvMem.setText(doc.getString("member_name")); 
                tvMem.setPadding(20, 16, 20, 16); 
                tvMem.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); 
                tvMem.setTextColor(Color.parseColor("#1E293B")); 
                tvMem.setGravity(Gravity.CENTER); 
                memLayout.addView(tvMem);
                
                String note = doc.getString("notes");
                if (note != null && !note.trim().isEmpty()) {
                    tvMem.setPadding(20, 16, 20, 0); 
                    TextView tvNote = new TextView(this);
                    tvNote.setText("📝 " + note);
                    tvNote.setTextSize(11);
                    tvNote.setTextColor(Color.parseColor("#64748B"));
                    tvNote.setPadding(20, 0, 20, 16);
                    tvNote.setGravity(Gravity.CENTER);
                    memLayout.addView(tvNote);
                }
                tr.addView(memLayout);
                
                TextView tvInst = new TextView(this); tvInst.setText("Inst. " + doc.getLong("installment_num")); tvInst.setPadding(20, 16, 20, 16); tvInst.setTextColor(Color.parseColor("#475569")); tr.addView(tvInst);
                
                TextView tvAdv = new TextView(this); 
                tvAdv.setText("₹" + String.format(Locale.getDefault(), "%,.1f", doc.getDouble("advance_amount"))); 
                tvAdv.setPadding(20, 16, 20, 16); 
                tvAdv.setTypeface(null, Typeface.BOLD); 
                tvAdv.setTextColor(Color.parseColor("#E11D48")); 
                tvAdv.setGravity(Gravity.CENTER); 
                tr.addView(tvAdv);
                
                TextView tvRate = new TextView(this); tvRate.setText("₹" + String.format(Locale.getDefault(), "%,.1f", doc.getDouble("new_amount"))); tvRate.setPadding(20, 16, 20, 16); tvRate.setTypeface(null, Typeface.BOLD); tvRate.setTextColor(Color.parseColor("#047857")); tvRate.setGravity(Gravity.CENTER); tr.addView(tvRate);

                final QueryDocumentSnapshot finalDoc = doc; 
                tr.setOnLongClickListener(v -> {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Advance Options")
                            .setItems(new String[]{"Edit Advance Record"}, (dialogInterface, which) -> {
                                if (which == 0) {
                                    dialogEngine.showEditAdvanceDialog(finalDoc);
                                }
                            })
                            .show();
                    return true;
                });

                tlAdvancesTable.addView(tr);
            }
        });
    }

    public void refreshGlobalNoteCard() {
        android.content.SharedPreferences prefs = getSharedPreferences("ChitPrefs", MODE_PRIVATE);
        java.util.Set<String> notesSet = prefs.getStringSet("global_notes_set", new java.util.HashSet<>());
        
        currentGlobalNotesList.clear();
        currentGlobalNotesList.addAll(notesSet);

        if (notesAnimationRunnable != null) {
            notesAnimationHandler.removeCallbacks(notesAnimationRunnable);
        }

        if (globalNoteContainer == null) {
            globalNoteContainer = new LinearLayout(this);
            globalNoteContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(50, 40, 50, 30); 
            globalNoteContainer.setLayoutParams(lp);

            ViewGroup targetGroup = (ViewGroup) tabContainerCollect;
            if (targetGroup instanceof ScrollView) {
                targetGroup = (ViewGroup) targetGroup.getChildAt(0);
            }
            targetGroup.addView(globalNoteContainer, 0);
        }

        globalNoteContainer.removeAllViews();

        if (currentGlobalNotesList.isEmpty()) {
            globalNoteContainer.setVisibility(View.GONE);
            return;
        }

        globalNoteContainer.setVisibility(View.VISIBLE);
        
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(50, 40, 50, 40);
        card.setGravity(Gravity.CENTER_VERTICAL);

        if (noteCardAnimator != null) {
            noteCardAnimator.cancel();
        }

        float noteRadius = 16 * getResources().getDisplayMetrics().density; 
        final PremiumUI.SnakeBorderDrawable snakeBg = new PremiumUI.SnakeBorderDrawable(Color.parseColor("#0F172A"), Color.WHITE, noteRadius);
        card.setBackground(snakeBg);

        card.setElevation(12f); 
        card.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), noteRadius);
            }
        });
        card.setClipToOutline(true);

        noteCardAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        noteCardAnimator.setDuration(1600); 
        noteCardAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        noteCardAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
        noteCardAnimator.addUpdateListener(animation -> {
            snakeBg.setAnimationProgress(-(float) animation.getAnimatedValue());
            card.postInvalidateOnAnimation();
        });
        noteCardAnimator.start();

        TextView icon = new TextView(this);
        icon.setText("📌");
        icon.setTextSize(20);
        icon.setPadding(0, 0, 30, 0);
        card.addView(icon);

        TextView tvNote = new TextView(this);
        tvNote.setTextColor(Color.parseColor("#1E293B"));
        tvNote.setTextSize(14f);
        tvNote.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvNote.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(tvNote);

        globalNoteContainer.addView(card);

        card.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startTouchX;

            @Override
            public boolean onTouch(View view, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = view.getTranslationX();
                        startTouchX = event.getRawX();
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dX = event.getRawX() - startTouchX;
                        if (dX > 0) { 
                            view.setTranslationX(startX + dX);
                            view.setAlpha(1f - (dX / view.getWidth()));
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        if (view.getTranslationX() > view.getWidth() / 3) {
                            view.animate().translationX(view.getWidth()).alpha(0).setDuration(250)
                                    .withEndAction(() -> {
                                        globalNoteContainer.setVisibility(View.GONE);
                                        view.setTranslationX(0);
                                        view.setAlpha(1);
                                    }).start();
                        } else { 
                            view.animate().translationX(0).alpha(1).setDuration(250).start();
                        }
                        return true;
                }
                return false;
            }
        });

        currentGlobalNoteIndex = 0;
        tvNote.setText(currentGlobalNotesList.get(currentGlobalNoteIndex));
        
        if (currentGlobalNotesList.size() > 1) {
            notesAnimationRunnable = new Runnable() {
                @Override
                public void run() {
                    tvNote.animate().alpha(0f).setDuration(600).withEndAction(() -> {
                        currentGlobalNoteIndex = (currentGlobalNoteIndex + 1) % currentGlobalNotesList.size();
                        tvNote.setText(currentGlobalNotesList.get(currentGlobalNoteIndex));
                        tvNote.animate().alpha(1f).setDuration(600).start();
                    }).start();
                    notesAnimationHandler.postDelayed(this, 4500);
                }
            };
            notesAnimationHandler.postDelayed(notesAnimationRunnable, 4500);
        }
    }

    public void showDeleteChitSelectionDialog() {
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
                        LedgerComponents.CloudChitItem chosenChit = globalChitsList.get(which);
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
}
