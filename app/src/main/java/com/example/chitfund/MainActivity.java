package com.example.chitfund;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database
        dbHelper = new DatabaseHelper(this);

        // Mock Example: Create a 50,000 Chit Fund Group for 50 Months (50 members)
        // Base monthly contribution per person = 1,000
        long groupId = dbHelper.createGroup("Premium June Group", 50000, 50);

        // Mock Auction: Someone bids a discount of 10,000 to take the prize money early.
        // Manager takes a 5% commission of the total value (5% of 50,000 = 2,500).
        double winningBid = 10000; 
        int totalMembers = 50;
        double managerCommissionPercent = 5.0; 
        double baseContribution = 50000 / totalMembers;

        double dividend = ChitCalculator.calculateDividend(winningBid, totalMembers, managerCommissionPercent, 50000);
        double actualNextPayment = ChitCalculator.calculateActualPayment(baseContribution, dividend);

        // Display the data calculation on screen
        TextView welcomeText = findViewById(R.id.welcomeText);
        String displayStats = "Chit Group Created ID: " + groupId + "\n\n"
                + "--- Month 1 Auction Stats ---\n"
                + "Base Contribution: ₹" + baseContribution + "\n"
                + "Winning Bid Discount: ₹" + winningBid + "\n"
                + "Dividend Per Member: ₹" + dividend + "\n"
                + "Actual Next Month Payment: ₹" + actualNextPayment;
                
        welcomeText.setText(displayStats);
    }
}
