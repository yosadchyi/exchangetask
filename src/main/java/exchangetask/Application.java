package exchangetask;

import java.util.Random;

public class Application {
    public static void main(final String... args) throws RequestRejectedException {
        final int nOperations = 10000000;
        int orderId = 0;
        final Random random = new Random();
        final Exchange exchange = new Exchange();
        final long startTime = System.nanoTime();

        for (int i = 0; i < nOperations; i++) {
            exchange.send(orderId++, random.nextBoolean(),
                    random.nextInt(9990) + 10,
                    random.nextInt(9990) + 10);
            if (i > 0 && i % 1000000 == 0) {
                printStats(startTime, i);
            }
        }
        printStats(startTime, nOperations);
    }

    private static void printStats(final long startTime, final int i) {
        final long endTime = System.nanoTime();
        final long timeElapsed = endTime - startTime;

        System.out.printf("ops: %d, time: %dns, %dns per action%n", i, timeElapsed, timeElapsed / i);
    }
}
