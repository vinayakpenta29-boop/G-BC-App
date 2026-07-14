package com.example.chitfund;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Locale;

public class PremiumUI {

    public static class SnakeBorderDrawable extends android.graphics.drawable.Drawable {
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

    public static void showPremiumChitSummaryDialog(Context context, String name, String startDate, String freq, int maxInst, String activeInstStr, ArrayList<String> members, double curDues, double pastDues, double grossDues, double totalAdvances, ArrayList<Double> planBreakdown, ArrayList<Integer> pendingSteps, double totalPlanAmount, double totalPaid, double balanceAmount, ArrayList<String> advanceLogs, int paidInstCount, int remainingInstCount, ArrayList<Integer> activeSteps) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(60, 60, 60, 60);
        scrollView.addView(mainLayout);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(name);
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        mainLayout.addView(tvTitle);

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText("Workspace Overview Summary");
        tvSubtitle.setTextSize(14);
        tvSubtitle.setTextColor(Color.parseColor("#64748B"));
        tvSubtitle.setPadding(0, 0, 0, 50);
        mainLayout.addView(tvSubtitle);

        float cornerRadius = 32f;
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 30);

        LinearLayout pendingCard = new LinearLayout(context);
        pendingCard.setOrientation(LinearLayout.VERTICAL);
        pendingCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable pendBg = new android.graphics.drawable.GradientDrawable();
        pendBg.setColor(Color.parseColor("#EFF6FF")); 
        pendBg.setCornerRadius(cornerRadius);
        pendingCard.setBackground(pendBg);
        pendingCard.setLayoutParams(cardParams);
        
        TextView tvPendLbl = new TextView(context);
        tvPendLbl.setText("Total Pending Dues");
        tvPendLbl.setTextColor(Color.parseColor("#1D4ED8"));
        tvPendLbl.setTextSize(13);
        tvPendLbl.setTypeface(null, Typeface.BOLD);
        
        TextView tvPendVal = new TextView(context);
        tvPendVal.setText("₹" + String.format(Locale.getDefault(), "%,.1f", grossDues));
        tvPendVal.setTextColor(Color.parseColor("#1E3A8A"));
        tvPendVal.setTextSize(26);
        tvPendVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        
        pendingCard.addView(tvPendLbl);
        pendingCard.addView(tvPendVal);
        
        TextView tvPendSteps = new TextView(context);
        String pStepsStr = pendingSteps.isEmpty() ? "None" : pendingSteps.toString().replace("[", "").replace("]", "");
        tvPendSteps.setText("Pending Installments: " + pStepsStr);
        tvPendSteps.setTextColor(Color.parseColor("#2563EB"));
        tvPendSteps.setTextSize(13);
        tvPendSteps.setPadding(0, 10, 0, 0);
        pendingCard.addView(tvPendSteps);

        mainLayout.addView(pendingCard);

        LinearLayout finGrid = new LinearLayout(context);
        finGrid.setOrientation(LinearLayout.HORIZONTAL);
        finGrid.setWeightSum(2);
        finGrid.setLayoutParams(cardParams);

        LinearLayout curCard = new LinearLayout(context);
        curCard.setOrientation(LinearLayout.VERTICAL);
        curCard.setPadding(40, 40, 40, 40);
        android.graphics.drawable.GradientDrawable curBg = new android.graphics.drawable.GradientDrawable();
        curBg.setColor(Color.parseColor("#F1F5F9")); 
        curBg.setCornerRadius(cornerRadius);
        curCard.setBackground(curBg);
        
        TextView tvCurLbl = new TextView(context);
        tvCurLbl.setText("Active Dues");
        tvCurLbl.setTextColor(Color.parseColor("#475569"));
        tvCurLbl.setTextSize(12);
        TextView tvCurVal = new TextView(context);
        tvCurVal.setText("₹" + String.format(Locale.getDefault(), "%,.1f", curDues));
        tvCurVal.setTextColor(Color.parseColor("#0F172A"));
        tvCurVal.setTextSize(18);
        tvCurVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        curCard.addView(tvCurLbl);
        curCard.addView(tvCurVal);
        
        LinearLayout.LayoutParams halfLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        halfLeft.setMargins(0, 0, 15, 0);
        curCard.setLayoutParams(halfLeft);
        finGrid.addView(curCard);

        LinearLayout pastCard = new LinearLayout(context);
        pastCard.setOrientation(LinearLayout.VERTICAL);
        pastCard.setPadding(40, 40, 40, 40);
        android.graphics.drawable.GradientDrawable pastBg = new android.graphics.drawable.GradientDrawable();
        pastBg.setColor(Color.parseColor("#FFF7ED")); 
        pastBg.setCornerRadius(cornerRadius);
        pastCard.setBackground(pastBg);

        TextView tvPastLbl = new TextView(context);
        tvPastLbl.setText("Past Arrears");
        tvPastLbl.setTextColor(Color.parseColor("#C2410C"));
        tvPastLbl.setTextSize(12);
        TextView tvPastVal = new TextView(context);
        tvPastVal.setText("₹" + String.format(Locale.getDefault(), "%,.1f", pastDues));
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

        LinearLayout finSumCard = new LinearLayout(context);
        finSumCard.setOrientation(LinearLayout.VERTICAL);
        finSumCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable finSumBg = new android.graphics.drawable.GradientDrawable();
        finSumBg.setColor(Color.parseColor("#F0FDF4")); 
        finSumBg.setCornerRadius(cornerRadius);
        finSumCard.setBackground(finSumBg);
        finSumCard.setLayoutParams(cardParams);
        
        String[] finLabels = {"Total Plan Amount", "Total Amount Paid", "Balance to be Paid", "Paid Installments", "Remaining Installments"};
        String[] finValues = {
            "₹" + String.format(Locale.getDefault(), "%,.1f", totalPlanAmount),
            "₹" + String.format(Locale.getDefault(), "%,.1f", totalPaid),
            "₹" + String.format(Locale.getDefault(), "%,.1f", balanceAmount),
            String.valueOf(paidInstCount),
            String.valueOf(remainingInstCount)
        };
        String[] finColors = {"#0F172A", "#15803D", "#B91C1C", "#15803D", "#B91C1C"};

        for(int i=0; i<5; i++){
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);
            
            TextView lbl = new TextView(context);
            lbl.setText(finLabels[i]);
            lbl.setTextColor(Color.parseColor("#475569"));
            lbl.setTextSize(14);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            TextView val = new TextView(context);
            val.setText(finValues[i]);
            val.setTextColor(Color.parseColor(finColors[i]));
            val.setTextSize(15);
            val.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            
            row.addView(lbl);
            row.addView(val);
            finSumCard.addView(row);
        }
        mainLayout.addView(finSumCard);

        LinearLayout advCard = new LinearLayout(context);
        advCard.setOrientation(LinearLayout.VERTICAL);
        advCard.setPadding(50, 40, 50, 40);
        android.graphics.drawable.GradientDrawable advBg = new android.graphics.drawable.GradientDrawable();
        advBg.setColor(Color.parseColor("#FEF2F2")); 
        advBg.setCornerRadius(cornerRadius);
        advCard.setBackground(advBg);
        
        TextView tvAdvLbl = new TextView(context);
        tvAdvLbl.setText("Total Advanced Payouts");
        tvAdvLbl.setTextColor(Color.parseColor("#991B1B"));
        tvAdvLbl.setTextSize(13);
        tvAdvLbl.setTypeface(null, Typeface.BOLD);
        
        TextView tvAdvVal = new TextView(context);
        tvAdvVal.setText("₹" + String.format(Locale.getDefault(), "%,.1f", totalAdvances));
        tvAdvVal.setTextColor(Color.parseColor("#DC2626"));
        tvAdvVal.setTextSize(26);
        tvAdvVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        
        advCard.addView(tvAdvLbl);
        advCard.addView(tvAdvVal);
        
        if (!advanceLogs.isEmpty()) {
            View divider = new View(context);
            divider.setBackgroundColor(Color.parseColor("#FECACA"));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
            divParams.setMargins(0, 20, 0, 20);
            divider.setLayoutParams(divParams);
            advCard.addView(divider);
            
            for(String log : advanceLogs) {
                TextView tvLog = new TextView(context);
                tvLog.setText("• " + log);
                tvLog.setTextColor(Color.parseColor("#991B1B"));
                tvLog.setTextSize(13);
                advCard.addView(tvLog);
            }
        }
        advCard.setLayoutParams(cardParams);
        mainLayout.addView(advCard);

        LinearLayout infoLayout = new LinearLayout(context);
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
            LinearLayout infoRow = new LinearLayout(context);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setPadding(0, 10, 0, 10);
            
            TextView lbl = new TextView(context);
            lbl.setText(infoLabels[i]);
            lbl.setTextColor(Color.parseColor("#64748B"));
            lbl.setTextSize(14);
            lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            TextView val = new TextView(context);
            val.setText(infoValues[i]);
            val.setTextColor(Color.parseColor("#0F172A"));
            val.setTextSize(14);
            val.setTypeface(null, Typeface.BOLD);
            
            infoRow.addView(lbl);
            infoRow.addView(val);
            infoLayout.addView(infoRow);
        }
        mainLayout.addView(infoLayout);

        TextView tvMemTitle = new TextView(context);
        tvMemTitle.setText("Registered Members (" + members.size() + ")");
        tvMemTitle.setTextSize(15);
        tvMemTitle.setTypeface(null, Typeface.BOLD);
        tvMemTitle.setTextColor(Color.parseColor("#334155"));
        tvMemTitle.setPadding(0, 20, 0, 20);
        mainLayout.addView(tvMemTitle);

        LinearLayout memLayout = new LinearLayout(context);
        memLayout.setOrientation(LinearLayout.VERTICAL);
        memLayout.setPadding(50, 30, 50, 30);
        memLayout.setBackground(infoBg); 
        memLayout.setLayoutParams(cardParams);

        if(members.isEmpty()){
            TextView empty = new TextView(context);
            empty.setText("No members added yet.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            memLayout.addView(empty);
        } else {
            for(int i=0; i<members.size(); i++){
                TextView m = new TextView(context);
                m.setText("• " + members.get(i));
                m.setTextColor(Color.parseColor("#1E293B"));
                m.setTextSize(14);
                m.setPadding(0, 10, 0, 10);
                memLayout.addView(m);
            }
        }
        mainLayout.addView(memLayout);

        TextView tvPlanTitle = new TextView(context);
        tvPlanTitle.setText("Installment Plan Matrix");
        tvPlanTitle.setTextSize(15);
        tvPlanTitle.setTypeface(null, Typeface.BOLD);
        tvPlanTitle.setTextColor(Color.parseColor("#334155"));
        tvPlanTitle.setPadding(0, 20, 0, 20);
        mainLayout.addView(tvPlanTitle);

        LinearLayout planLayout = new LinearLayout(context);
        planLayout.setOrientation(LinearLayout.VERTICAL);
        planLayout.setPadding(50, 30, 50, 30);
        planLayout.setBackground(infoBg);
        planLayout.setLayoutParams(cardParams);

        if(planBreakdown == null || planBreakdown.isEmpty()){
            TextView empty = new TextView(context);
            empty.setText("No plan chart available.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            planLayout.addView(empty);
        } else {
            for(int i=0; i<planBreakdown.size(); i++){
                int currentStepNum = i + 1;
                LinearLayout rRow = new LinearLayout(context);
                rRow.setOrientation(LinearLayout.HORIZONTAL);
                rRow.setPadding(0, 10, 0, 10);
                
                TextView step = new TextView(context);
                step.setText("Step #" + currentStepNum);
                step.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                
                TextView amt = new TextView(context);
                amt.setText("₹" + String.format(Locale.getDefault(), "%,.1f", planBreakdown.get(i)));
                
                if (activeSteps != null && activeSteps.contains(currentStepNum)) {
                    step.setTextColor(Color.parseColor("#15803D"));
                    step.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                    amt.setTextColor(Color.parseColor("#15803D"));
                    amt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                } else {
                    step.setTextColor(Color.parseColor("#64748B"));
                    step.setTextSize(14);
                    amt.setTextColor(Color.parseColor("#0F172A"));
                    amt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                }
                
                rRow.addView(step);
                rRow.addView(amt);
                planLayout.addView(rRow);
            }
        }
        mainLayout.addView(planLayout);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(scrollView);
        builder.setPositiveButton("Dismiss Dashboard", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_window_bg);
    }
}
