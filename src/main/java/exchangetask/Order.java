package exchangetask;

import java.util.Objects;

public class Order {
    private final long id;
    private final boolean buy;
    private final int price;
    private int size;

    public Order(final long id, final boolean buy, final int price, final int size) {
        this.id = id;
        this.buy = buy;
        this.price = price;
        this.size = size;
    }

    public long getId() {
        return id;
    }

    public boolean isBuy() {
        return buy;
    }

    public boolean isSell() {
        return !buy;
    }

    public int getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" + "id=" + id + ", buy=" + buy + ", price=" + price + ", size=" + size + '}';
    }
}
