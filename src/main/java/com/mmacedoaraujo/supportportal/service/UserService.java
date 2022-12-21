package com.mmacedoaraujo.supportportal.service;

import com.mmacedoaraujo.supportportal.domain.User;

import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String username, String email);

    List<User> getUsers();

    User findUserByUsername(String username);

    User findUserByEmail(String email);
}
