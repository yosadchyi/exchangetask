package exchangetask;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

public class Exchange implements ExchangeInterface, QueryInterface {
    private long lastSequence = 1;
    private final Map<Long, Order> orderById = new HashMap<>();
    private final Map<Long, Order> executedOrdersById = new HashMap<>();
    private final Set<Long> cancelledOrderIds = new HashSet<>();
    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
            .reversed()
            .thenComparing(Order::getSequence));
    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>(Comparator.comparing(Order::getPrice)
            .thenComparing(Order::getSequence));

    @Override
    public void send(final long orderId,
                     final boolean isBuy,
                     final int price,
                     final int size) throws RequestRejectedException {
        if (orderById.containsKey(orderId) || executedOrdersById.containsKey(orderId)) {
            throw new RequestRejectedException(String.format("Order with id %d already exists!", orderId));
        }
        if (price <= 0) {
            throw new RequestRejectedException("Order has zero or lower price!");
        }
        if (size <= 0) {
            throw new RequestRejectedException("Order has zero or lower size!");
        }

        final Order order = new Order(lastSequence++, orderId, isBuy, price, size);

        if (order.isBuy()) {
            processOrder(order, sellOrders, (sellOrder) -> sellOrder.getPrice() <= order.getPrice());
        } else {
            processOrder(order, buyOrders, (buyOrder) -> order.getPrice() <= buyOrder.getPrice());
        }
    }

    private void processOrder(final Order order, final PriorityQueue<Order> orders, final Predicate<Order> filterPredicate) {
        while (!orders.isEmpty() && order.getSize() > 0) {
            final Order other = orders.peek();

            if (isCancelledOrder(other)) {
                orders.remove();
                continue;
            }
            if (!filterPredicate.test(other)) {
                break;
            }

            doExchange(order, other);

            if (other.isEmpty()) {
                markOrderExecuted(other);
                orders.remove();
            }
        }

        if (order.isEmpty()) {
            markOrderExecuted(order);
        } else {
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

    private void markOrderExecuted(final Order other) {
        executedOrdersById.put(other.getId(), other);
    }

    public boolean canCancelOrder(final long orderId) {
        if (executedOrdersById.containsKey(orderId)) {
            return false;
        }
        if (cancelledOrderIds.contains(orderId)) {
            return false;
        }
        return true;
    }

    @Override
    public void cancel(final long orderId) throws RequestRejectedException {
        if (executedOrdersById.containsKey(orderId)) {
            throw new RequestRejectedException(String.format("Order with id %d is already executed!", orderId));
        }
        final Order order = orderById.remove(orderId);

        if (order == null) {
            throw new RequestRejectedException(String.format("Order with id %d does not exists!", orderId));
        }

        orderById.remove(orderId);
        cancelledOrderIds.add(order.getId());
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

    private Integer getTotalSizeAtPriceInList(final int price, final Collection<Order> orders) {
        return orders.stream()
                .filter(this::isNotCancelledOrder)
                .filter(o -> o.getPrice() == price)
                .mapToInt(Order::getSize)
                .reduce(Integer::sum)
                .orElse(0);
    }

    @Override
    public int getHighestBuyPrice() throws RequestRejectedException {
        return buyOrders.stream()
                .filter(this::isNotCancelledOrder)
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No BUY orders present!"));
    }

    @Override
    public int getLowestSellPrice() throws RequestRejectedException {
        return sellOrders.stream()
                .filter(this::isNotCancelledOrder)
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No SELL orders present!"));
    }

    private boolean isCancelledOrder(final Order order) {
        return cancelledOrderIds.contains(order.getId());
    }

    private boolean isNotCancelledOrder(final Order order) {
        return !isCancelledOrder(order);
    }
}
