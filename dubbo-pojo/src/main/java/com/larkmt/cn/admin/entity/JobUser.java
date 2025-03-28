package com.larkmt.cn.admin.entity;

import java.io.Serializable;

/**
 * @author JiangWeiWei
 */
public class JobUser implements Serializable {

    private int id;
    private String username;
    private String password;
    private String role;
    private String permission;

    private String correlationId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // plugin
    public boolean validPermission(int jobGroup){
        if ("1".equals(this.role)) {
            return true;
        } else {
            if (this.permission != null && !this.permission.isEmpty()) {
                for (String permissionItem : this.permission.split(",")) {
                    if (String.valueOf(jobGroup).equals(permissionItem)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
