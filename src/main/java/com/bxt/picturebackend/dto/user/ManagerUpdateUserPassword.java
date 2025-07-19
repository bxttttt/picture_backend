package com.bxt.picturebackend.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class ManagerUpdateUserPassword implements Serializable {
    private static final long serialVersionUID = 8964668745545368554L;
    Long id;
    /**
     * 新密码
     */
    private String password;
}
