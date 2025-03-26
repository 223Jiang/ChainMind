package com.tencent.supersonic.auth.api.authentication.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserTokenReq {
    @NotBlank(message = "name can not be null")
    private String name;

    @NotBlank(message = "expireTime can not be null")
    private long expireTime;

}
