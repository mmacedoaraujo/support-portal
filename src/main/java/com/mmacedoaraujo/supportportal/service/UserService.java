package com.mmacedoaraujo.supportportal.service;

import com.mmacedoaraujo.supportportal.domain.User;
import com.mmacedoaraujo.supportportal.exception.domain.EmailExistException;
import com.mmacedoaraujo.supportportal.exception.domain.EmailNotFoundException;
import com.mmacedoaraujo.supportportal.exception.domain.UserNotFoundException;
import com.mmacedoaraujo.supportportal.exception.domain.UsernameExistException;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException;

    List<User> getUsers();

    User findByUsername(String username);

    User findByEmail(String email);

    User addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNonLocked, boolean isEnabled, MultipartFile profileImage) throws Exception;

    User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isEnabled, MultipartFile profileImage) throws Exception;

    void deleteUser(Long id);

    void resetPassword(String email) throws EmailNotFoundException, MessagingException;

    User updateProfileImage(String username, MultipartFile profileImage) throws Exception;
}
