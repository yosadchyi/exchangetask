package exchangetask;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeTest {
    @Test
    public void buyOrderShouldBeClosedIfSellOrderWithLowerPriceExists() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 5, 100);
        exchange.send(2, true, 10, 100);
        assertEquals(0, exchange.getTotalSizeAtPrice(10));
        assertEquals(0, exchange.getTotalSizeAtPrice(5));
    }

    @Test
    public void buyOrderShouldNotBeClosedIfSellOrderWithHigherPriceExists() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 15, 100);
        exchange.send(2, true, 10, 100);
        assertEquals(100, exchange.getTotalSizeAtPrice(10));
        assertEquals(100, exchange.getTotalSizeAtPrice(15));
    }

    @Test
    public void buyOrderShouldBeClosedPartiallyIfSizesAreDifferentButPricesAreMatching() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 5, 100);
        exchange.send(2, true, 10, 1000);
        assertEquals(900, exchange.getTotalSizeAtPrice(10));
        assertEquals(0, exchange.getTotalSizeAtPrice(5));
    }

    @Test
    public void sellOrderShouldBeClosedPartiallyIfSizesAreDifferentButPricesAreMatching() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 5, 1000);
        exchange.send(2, true, 10, 100);
        assertEquals(900, exchange.getTotalSizeAtPrice(5));
        assertEquals(0, exchange.getTotalSizeAtPrice(15));
    }

    @Test
    public void buyOrderShouldBeClosedIfThereAreSeveralSellOrdersWithMatchingPrice() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 5, 100);
        exchange.send(2, false, 9, 100);
        exchange.send(3, true, 10, 200);
        assertEquals(0, exchange.getTotalSizeAtPrice(10));
        assertEquals(0, exchange.getTotalSizeAtPrice(5));
        assertEquals(0, exchange.getTotalSizeAtPrice(9));
    }

    @Test
    public void sellOrderShouldBeClosedIfThereAreSeveralBuyOrdersWithMatchingPrice() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, true, 10, 100);
        exchange.send(2, true, 15, 150);
        exchange.send(3, false, 5, 200);
        assertEquals(0, exchange.getTotalSizeAtPrice(10));
        assertEquals(50, exchange.getTotalSizeAtPrice(15));
        assertEquals(0, exchange.getTotalSizeAtPrice(5));
    }

    @Test
    public void executedOrderCanNotBeCancelled() throws RequestRejectedException {
        final Exchange exchange = new Exchange();

        exchange.send(1, false, 5, 1000);
        exchange.send(2, true, 10, 100);
        assertEquals(900, exchange.getTotalSizeAtPrice(5));
        assertEquals(0, exchange.getTotalSizeAtPrice(10));
        assertThrows(RequestRejectedException.class, () -> exchange.cancel(2));
        assertDoesNotThrow(() -> exchange.cancel(1));
    }
}