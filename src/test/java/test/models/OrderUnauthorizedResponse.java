package test.models;

import lombok.Data;

@Data
public class OrderUnauthorizedResponse {
    private boolean success;
    private String name;
    private Order order;

    @Data
    public static class Order {
        private int number;
    }
}