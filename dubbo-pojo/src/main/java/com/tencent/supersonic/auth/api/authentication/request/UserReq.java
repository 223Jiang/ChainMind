package com.tencent.supersonic.auth.api.authentication.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserReq implements Serializable {

    @NotBlank(message = "name can not be null")
    private String name;

    @NotBlank(message = "password can not be null")
    private String password;

    private String correlationId;
}
