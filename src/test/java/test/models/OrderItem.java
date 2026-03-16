package test.models;

import lombok.Data;
import java.util.List;

@Data
public class OrderItem {
    private String _id;
    private List<String> ingredients;
    private String status;
    private String name;
    private String createdAt;
    private String updatedAt;
    private Integer number;
    private Integer price;
}