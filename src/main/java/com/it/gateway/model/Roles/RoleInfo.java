package com.it.gateway.model.Roles;

import com.it.gateway.model.General.GeneralInfo;

import lombok.Data;

@Data
public class RoleInfo extends GeneralInfo {
    private String organizationName;
    private Boolean isSystemRole;
    private Object permissions;
}
