package test.models;

import lombok.Data;

@Data
public class User {
    private String name;
    private String email;
    private String createdAt;
    private String updatedAt;
}