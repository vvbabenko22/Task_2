package test.models;

import lombok.Data;

@Data
public class OrderUnauthorizedRequest {
    private String[] ingredients;

    public OrderUnauthorizedRequest(String[] ingredients) {
        this.ingredients = ingredients;
    }
}