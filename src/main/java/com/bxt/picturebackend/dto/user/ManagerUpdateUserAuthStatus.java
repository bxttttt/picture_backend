package com.bxt.picturebackend.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class ManagerUpdateUserAuthStatus implements Serializable {


    private static final long serialVersionUID = -4086067262879086930L;
    Long id;
    String userRole;
}
