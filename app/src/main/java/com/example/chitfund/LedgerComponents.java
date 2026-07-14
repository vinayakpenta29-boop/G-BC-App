package com.example.chitfund;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Locale;

public class LedgerComponents {

    public static class CloudChitItem {
        public String id;
        public String name;
        public CloudChitItem(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    public static class LedgerTransaction {
        String date;
        String chitName;
        String memberName;
        String notes;
        long instNum;
        double amountPaid;
        public LedgerTransaction(String d, String c, String m, String n, long i, double a) {
            date = d; chitName = c; memberName = m; notes = n; instNum = i; amountPaid = a;
        }
    }

    public static class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder> {
        private ArrayList<LedgerTransaction> transactions = new ArrayList<>();

        public void updateData(ArrayList<LedgerTransaction> newTx) {
            this.transactions = newTx;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ledger_row, parent, false);
            return new LedgerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LedgerViewHolder holder, int position) {
            LedgerTransaction tx = transactions.get(position);
            holder.tvDate.setText(tx.date);
            holder.tvChit.setText(tx.chitName);
            holder.tvMem.setText(tx.memberName);
            
            if (tx.notes != null && !tx.notes.trim().isEmpty()) {
                holder.tvNote.setVisibility(View.VISIBLE);
                holder.tvNote.setText("📝 " + tx.notes);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }
            
            holder.tvInst.setText("Inst. " + tx.instNum);
            holder.tvAmt.setText("₹" + String.format(Locale.getDefault(), "%,.1f", tx.amountPaid));
            
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#F8FAFC"));
            }
        }

        @Override
        public int getItemCount() { return transactions.size(); }

        class LedgerViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvChit, tvMem, tvNote, tvInst, tvAmt;
            public LedgerViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.rowDate);
                tvChit = itemView.findViewById(R.id.rowChit);
                tvMem = itemView.findViewById(R.id.rowMem);
                tvNote = itemView.findViewById(R.id.rowNote);
                tvInst = itemView.findViewById(R.id.rowInst);
                tvAmt = itemView.findViewById(R.id.rowAmt);
            }
        }
    }
}
