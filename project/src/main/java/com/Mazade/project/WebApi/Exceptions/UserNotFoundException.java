package com.Mazade.project.WebApi.Exceptions;

public class UserNotFoundException extends  Exception {
    public UserNotFoundException(String message) {
        super(message);
    }
}
