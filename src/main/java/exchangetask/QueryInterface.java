package exchangetask;

public interface QueryInterface {
    // Return sum of sizes of resting orders at <price> or zero
    int getTotalSizeAtPrice(int price) throws RequestRejectedException;
    // Return the highest price with at least one resting Buy order
    int getHighestBuyPrice() throws RequestRejectedException;
    // Return the lowest price with at least one resting Sell order
    int getLowestSellPrice() throws RequestRejectedException;
}
