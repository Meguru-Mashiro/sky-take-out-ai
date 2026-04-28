package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
}
