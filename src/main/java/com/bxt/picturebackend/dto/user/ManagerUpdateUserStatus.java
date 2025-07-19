package com.bxt.picturebackend.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class ManagerUpdateUserStatus implements Serializable {

    private static final long serialVersionUID = 8176119089406298165L;

    Long id;
    Integer status;
}
