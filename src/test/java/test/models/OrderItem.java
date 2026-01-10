package test.models;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class OrderItem {
    private String _id;
    private List<String> ingredients;
    private String status;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private Integer number;
}