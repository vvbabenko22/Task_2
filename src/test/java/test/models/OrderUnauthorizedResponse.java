package test.models;

import lombok.Data;
import java.util.List;

@Data
public class OrderUnauthorizedResponse {
    private boolean success;
    private String name;
    private Order order;

    @Data
    public static class Order {
        private String _id;
        private List<Ingredient> ingredients;
        private String status;
        private String name;
        private String createdAt;
        private String updatedAt;
        private Integer number;
        private Integer price;
        private User owner;
    }
}