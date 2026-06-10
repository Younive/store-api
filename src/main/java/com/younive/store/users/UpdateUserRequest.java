package com.younive.store.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 255, message = "Name must be less than 255 char")
    private String name;

    @Email(message = "Email must be valid")
    private String email;
}
