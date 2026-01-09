package test.models;

import lombok.Data;

@Data
public class LoginResponse {
    private Integer statusCode; // Новое поле для хранения статуса ответа
    private Boolean success;
    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private String message;

    // Геттеры и сеттеры (используется lombok)

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    // Другие геттеры и сеттеры генерируются автоматически с помощью lombok
}