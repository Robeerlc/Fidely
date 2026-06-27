package com.fidely.dto.request;

import lombok.Data;

@Data
public class CreateEmployeeRequest {
    private String name;
    private String email;
    private String password;
}