package exchangetask;

import java.util.Random;

public class Application {
    public static void main(final String... args) throws RequestRejectedException {
        final int nOperations = 100000000;
        int orderId = 0;
        final Random random = new Random();
        final Exchange exchange = new Exchange();
        long totalTimeElapsed = 0;
        int numPlacedOrders = 0;

        for (int i = 0; i < nOperations; i++) {
            final int price = random.nextInt(9990) + 10;
            final int size = random.nextInt(9990) + 10;
            final double probabilityToSend = Math.exp(-numPlacedOrders * Math.log(2) / 2e4);
            final double sendRoll = random.nextDouble();
            final boolean cancelOrder = probabilityToSend < sendRoll;
            final long orderToCancel = cancelOrder ? random.nextInt(numPlacedOrders) : -1;
            final boolean buy = random.nextBoolean();

            final long startTime = System.nanoTime();
            if (cancelOrder) {
                try {
                    exchange.cancel(orderToCancel);
                } catch (RequestRejectedException ex) {
                }
            } else {
                exchange.send(orderId++, buy, price, size);
                numPlacedOrders++;
            }
            final long endTime = System.nanoTime();
            totalTimeElapsed += endTime - startTime;

            if (i > 0 && i % 1000000 == 0) {
                printStats(totalTimeElapsed, i);
            }
        }
        printStats(totalTimeElapsed, nOperations);
    }

    private static void printStats(final long timeElapsed, final int i) {
        System.out.printf("ops: %d, time: %dns, %dns per action%n", i, timeElapsed, timeElapsed / i);
    }
}
