package test.models;

import lombok.Data;

import java.util.List;

@Data
public class GetOrdersResponse {
    private Boolean success;
    private List<OrderItem> orders;
    private Long total;
    private Long totalToday;
}