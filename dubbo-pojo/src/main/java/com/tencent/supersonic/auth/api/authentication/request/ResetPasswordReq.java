package com.tencent.supersonic.auth.api.authentication.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * ChainMind
 *
 * @author WeiWei
 * @version V5.0.0
 * @date 2025/3/13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordReq {
    @NotBlank(message = "userName can not be null")
    private String userName;

    @NotBlank(message = "password can not be null")
    private String password;

    @NotBlank(message = "newPassword can not be null")
    private String newPassword;
}
