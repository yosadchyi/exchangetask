package exchangetask;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;

public class Exchange implements ExchangeInterface, QueryInterface {
    private static final RequestRejectedException ORDER_DOES_NOT_EXISTS = new RequestRejectedException("Order does " +
            "not exists!");
    private static final RequestRejectedException ORDER_ALREADY_EXISTS = new RequestRejectedException("Order already " +
            "exists!");
    private static final RequestRejectedException ORDER_HAS_INVALID_SIZE = new RequestRejectedException("Order has " +
            "zero or lower size!");
    private long lastSequence = 1;
    private final Map<Long, Order> orderById = new HashMap<>();
    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(Comparator.comparingInt(Order::getPrice)
            .reversed()
            .thenComparingLong(Order::getSequence));
    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>(Comparator.comparingInt(Order::getPrice)
            .thenComparingLong(Order::getSequence));

    @Override
    public void send(final long orderId,
                     final boolean isBuy,
                     final int price,
                     final int size) throws RequestRejectedException {
        if (orderById.containsKey(orderId)) {
            throw ORDER_ALREADY_EXISTS;
        }
        if (size <= 0) {
            throw ORDER_HAS_INVALID_SIZE;
        }

        final Order order = new Order(lastSequence++, orderId, isBuy, price, size);

        if (order.isBuy()) {
            processOrder(order, sellOrders, sellOrder -> sellOrder.getPrice() <= order.getPrice());
        } else {
            processOrder(order, buyOrders, buyOrder -> order.getPrice() <= buyOrder.getPrice());
        }
    }

    private void processOrder(final Order order,
                              final PriorityQueue<Order> orders,
                              final Predicate<Order> filterPredicate) {
        while (!orders.isEmpty() && order.getSize() > 0) {
            final Order other = orders.peek();

            if (other.isCancelled()) {
                orders.remove();
                continue;
            }
            if (!filterPredicate.test(other)) {
                break;
            }

            doExchange(order, other);

            if (other.isEmpty()) {
                orders.remove();
            }
        }

        if (!order.isEmpty()) {
            addOrder(order);
        }
    }

    private void doExchange(final Order order, final Order other) {
        final int sizeLeft = order.getSize();
        final int otherSize = other.getSize();

        other.setSize(Math.max(0, otherSize - sizeLeft));
        order.setSize(Math.max(0, sizeLeft - otherSize));
    }

    private void addOrder(final Order order) {
        final Collection<Order> orders = getOrdersForOrder(order);

        orders.add(order);
        orderById.put(order.getId(), order);
    }

    @Override
    public void cancel(final long orderId) throws RequestRejectedException {
        final Order order = orderById.remove(orderId);

        if (order == null) {
            throw ORDER_DOES_NOT_EXISTS;
        }

        order.setCancelled(true);
    }

    private Collection<Order> getOrdersForOrder(final Order order) {
        return order.isBuy() ? buyOrders : sellOrders;
    }

    @Override
    public int getTotalSizeAtPrice(final int price) throws RequestRejectedException {
        if (price <= 0) {
            throw new RequestRejectedException("Invalid price.");
        }
        return getTotalSizeAtPriceInList(price, buyOrders) + getTotalSizeAtPriceInList(price, sellOrders);
    }

    private Integer getTotalSizeAtPriceInList(final int price, final PriorityQueue<Order> orders) {
        return orders.stream()
                .filter(Order::isNotCancelled)
                .filter(o -> o.getPrice() == price)
                .mapToInt(Order::getSize)
                .reduce(Integer::sum)
                .orElse(0);
    }

    @Override
    public int getHighestBuyPrice() throws RequestRejectedException {
        return buyOrders.stream()
                .filter(Order::isNotCancelled)
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No BUY orders present!"));
    }

    @Override
    public int getLowestSellPrice() throws RequestRejectedException {
        return sellOrders.stream()
                .filter(Order::isNotCancelled)
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No SELL orders present!"));
    }
}
