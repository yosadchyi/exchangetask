package exchangetask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Exchange implements ExchangeInterface, QueryInterface {
    private final Map<Long, Order> orderById = new HashMap<>();
    private final Map<Long, Order> executedOrdersById = new HashMap<>();
    private final List<Order> buyOrders = new ArrayList<>();
    private final List<Order> sellOrders = new ArrayList<>();

    @Override
    public void send(final long orderId,
                     final boolean isBuy,
                     final int price,
                     final int size) throws RequestRejectedException {
        if (orderById.containsKey(orderId)) {
            throw new RequestRejectedException(String.format("Order with id %d already exists!", orderId));
        }
        if (price == 0) {
            throw new RequestRejectedException("Order has zero price!");
        }
        if (size == 0) {
            throw new RequestRejectedException("Order has zero size!");
        }

        final Order order = new Order(orderId, isBuy, price, size);

        if (order.isBuy()) {
            processOrder(order, sellOrders, (sellOrder) -> sellOrder.getPrice() <= order.getPrice());
        } else {
            processOrder(order, buyOrders, (buyOrder) -> order.getPrice() <= buyOrder.getPrice());
        }
    }

    private void processOrder(final Order order, final List<Order> orders, final Predicate<Order> filterPredicate) {
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
        if (order.isBuy()) {
            buyOrders.add(order);
        } else {
            sellOrders.add(order);
        }
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

        final List<Order> orders = order.isBuy() ? buyOrders : sellOrders;

        orderById.remove(orderId);
        orders.remove(order);
    }

    @Override
    public int getTotalSizeAtPrice(final int price) throws RequestRejectedException {
        return getTotalSizeAtPriceInList(price, buyOrders) + getTotalSizeAtPriceInList(price, sellOrders);
    }

    private Integer getTotalSizeAtPriceInList(final int price, final List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getPrice() == price)
                .map(Order::getSize)
                .reduce(Integer::sum)
                .orElse(0);
    }

    @Override
    public int getHighestBuyPrice() throws RequestRejectedException {
        return buyOrders.stream()
                .max(Comparator.comparing(Order::getPrice))
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No BUY orders present!"));
    }

    @Override
    public int getLowestSellPrice() throws RequestRejectedException {
        return sellOrders.stream()
                .min(Comparator.comparing(Order::getPrice))
                .map(Order::getPrice)
                .orElseThrow(() -> new RequestRejectedException("No SELL orders present!"));
    }
}
