package com.example.chitfund;

public class ChitCalculator {

    /**
     * Calculates the dividend distributed to each member after an auction.
     * * @param winningBid The discount amount the winner gave up to get the pot.
     * @param totalMembers Total number of members in the chit group.
     * @param organizerCommissionPct Percentage taken by the manager (e.g., 5.0 for 5%)
     * @param totalChitValue The total value of the chit group fund.
     * @return The dividend amount each member receives to reduce their next premium.
     */
    public static double calculateDividend(double winningBid, int totalMembers, double organizerCommissionPct, double totalChitValue) {
        double commission = totalChitValue * (organizerCommissionPct / 100.0);
        double poolToDistribute = winningBid - commission;
        
        if (poolToDistribute <= 0) return 0;
        return poolToDistribute / totalMembers;
    }

    /**
     * Calculates what a member actually has to pay for the next month.
     */
    public static double calculateActualPayment(double baseContribution, double dividend) {
        return baseContribution - dividend;
    }
}
