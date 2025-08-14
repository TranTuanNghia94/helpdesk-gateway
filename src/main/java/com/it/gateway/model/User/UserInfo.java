package com.it.gateway.model.User;

import java.util.UUID;

import lombok.Data;

@Data
public class UserInfo {
    private UUID id;
    private String organizationName;
    private String departmentName;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String displayName;
    private String employeeId;
    private String phone;
    private String mobile;
    private String jobTitle;
    private String managerId;
    private String location;
    private String timezone;
    private String language;
    private String avatarUrl;
    private String lastLoginAt;
    private Object skills;
    private Object workSchedule;
    private Object preferences;
    private String status;
}
