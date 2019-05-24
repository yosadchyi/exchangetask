package exchangetask;

import java.util.Random;

public class Application {
    public static void main(final String... args) throws RequestRejectedException {
        final int nOperations = 10000000;
        int orderId = 0;
        final Random random = new Random();
        final Exchange exchange = new Exchange();
        long totalTimeElapsed = 0;

        for (int i = 0; i < nOperations; i++) {
            final int price = random.nextInt(9990) + 10;
            final int size = random.nextInt(9990) + 10;

            final long startTime = System.nanoTime();
            exchange.send(orderId++, random.nextBoolean(), price, size);
            final long endTime = System.nanoTime();
            totalTimeElapsed += endTime - startTime;

            if (i > 0 && i % 1000000 == 0) {
                printStats(totalTimeElapsed, i + 1);
            }
        }
        printStats(totalTimeElapsed, nOperations);
    }

    private static void printStats(final long timeElapsed, final int i) {
        System.out.printf("ops: %d, time: %dns, %dns per action%n", i, timeElapsed, timeElapsed / i);
    }
}
