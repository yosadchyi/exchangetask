package exchangetask;

public interface ExchangeInterface {
    void send(long orderId, boolean isBuy, int price, int size) throws RequestRejectedException;
    void cancel(long orderId) throws RequestRejectedException;
}
