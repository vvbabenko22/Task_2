package test.models;

import lombok.Data;

@Data
public class LoginResponse {
    private Integer statusCode;
    private Boolean success;
    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private String message;

    // Геттеры и сеттеры

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

}