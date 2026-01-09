
package test.models;

import lombok.Data;

@Data
public class UpdateUserResponse {
    private Boolean success;
    private UserInfo user;
}