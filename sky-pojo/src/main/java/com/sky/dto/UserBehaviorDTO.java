package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBehaviorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long dishId;
    private Long categoryId;
    private String behaviorType;
}
