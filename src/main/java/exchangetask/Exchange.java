package exchangetask;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Exchange implements ExchangeInterface, QueryInterface {
    private long lastSequence = 1;
    private final Map<Long, Order> orderById = new HashMap<>();
    private final Map<Long, Order> executedOrdersById = new HashMap<>();
    private final Collection<Order> buyOrders = new TreeSet<>(Comparator.comparing(Order::getPrice)
            .reversed()
            .thenComparing(Order::getSequence));
    private final Collection<Order> sellOrders = new TreeSet<>(Comparator.comparing(Order::getPrice)
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

    private void processOrder(final Order order, final Collection<Order> orders, final Predicate<Order> filterPredicate) {
        final List<Order> executedOrders = orders.stream()
                .filter(filterPredicate)
                .peek(other -> doExchange(order, other))
                .filter(Order::isEmpty)
                .peek(this::markOrderExecuted)
                .collect(Collectors.toList());

        orders.removeAll(executedOrders);

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

    @Override
    public void cancel(final long orderId) throws RequestRejectedException {
        if (executedOrdersById.containsKey(orderId)) {
            throw new RequestRejectedException(String.format("Order with id %d is already executed!", orderId));
        }
        final Order order = orderById.remove(orderId);

        if (order == null) {
            throw new RequestRejectedException(String.format("Order with id %d does not exists!", orderId));
        }

        final Collection<Order> orders = getOrdersForOrder(order);

        orderById.remove(orderId);
        orders.remove(order);
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
                .filter(o -> o.getPrice() == price)
                .mapToInt(Order::getSize)
                .reduce(Integer::sum)
                .orElse(0);
    }

    @Override
    public int getHighestBuyPrice() throws RequestRejectedException {
        return buyOrders.stream()
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No BUY orders present!"));
    }

    @Override
    public int getLowestSellPrice() throws RequestRejectedException {
        return sellOrders.stream()
                .findFirst()
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No SELL orders present!"));
    }
}
