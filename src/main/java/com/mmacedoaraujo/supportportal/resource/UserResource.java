package com.mmacedoaraujo.supportportal.resource;

import com.mmacedoaraujo.supportportal.domain.HttpResponse;
import com.mmacedoaraujo.supportportal.domain.User;
import com.mmacedoaraujo.supportportal.domain.UserPrincipal;
import com.mmacedoaraujo.supportportal.exception.ExceptionHandling;
import com.mmacedoaraujo.supportportal.exception.domain.EmailExistException;
import com.mmacedoaraujo.supportportal.exception.domain.EmailNotFoundException;
import com.mmacedoaraujo.supportportal.exception.domain.UserNotFoundException;
import com.mmacedoaraujo.supportportal.exception.domain.UsernameExistException;
import com.mmacedoaraujo.supportportal.repository.UserRepository;
import com.mmacedoaraujo.supportportal.service.UserService;
import com.mmacedoaraujo.supportportal.utility.JWTTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static com.mmacedoaraujo.supportportal.constant.FileConstant.*;
import static com.mmacedoaraujo.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = {"/", "/users"})
@AllArgsConstructor
public class UserResource extends ExceptionHandling {
    public static final String EMAIL_WITH_NEW_PASSWORD_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
    private final UserRepository userRepository;

    private final UserService userService;
    private final AuthenticationManager userResourceAuthenticationManager;
    private final JWTTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        authenticate(user.getUsername(), user.getPassword());
        User authenticatedUser = userService.findByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(authenticatedUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(authenticatedUser, jwtHeader, OK);
    }


    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        User registeredUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            @RequestParam("isEnabled") String isEnabled,
            @RequestParam("isNonLocked") String isNonLocked,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage)
            throws Exception {

        User newUser = userService.addNewUser(firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isEnabled), profileImage);

        return new ResponseEntity<>(newUser, HttpStatus.CREATED);

    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<User> update(
            @RequestParam("currentUsername") String currentUsername,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            @RequestParam("isEnabled") String isEnabled,
            @RequestParam("isNonLocked") String isNonLocked,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage)
            throws Exception {

        User updatedUser = userService.updateUser(currentUsername, firstName, lastName, username, email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isEnabled), profileImage);

        return new ResponseEntity<>(updatedUser, HttpStatus.NO_CONTENT);

    }

    @GetMapping("/find/{username}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<User> findByUsername(@PathVariable("username") String username) {
        User userFoundbyUsername = userService.findByUsername(username);

        return new ResponseEntity<>(userFoundbyUsername, OK);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> usersList = userService.getUsers();
        return new ResponseEntity<>(usersList, OK);
    }


    @GetMapping("/resetpassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return response(OK, EMAIL_WITH_NEW_PASSWORD_SENT + email);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return response(HttpStatus.NO_CONTENT, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updatePorfileImage(
            @RequestParam("username") String username,
            @RequestParam(value = "profileImage") MultipartFile profileImage)
            throws Exception {

        User user = userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, HttpStatus.NO_CONTENT);

    }

    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + fileName));
    }

    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTemporaryProfileImage(@PathVariable("username") String username) throws IOException {
        URL url = new URL("https://avatars.dicebear.com/api/micah/:" + username + ".jpg");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "Firefox");

        try (InputStream inputStream = conn.getInputStream()) {
            int n = 0;
            byte[] buffer = new byte[1024];
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
            }
        }
        return output.toByteArray();
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {

        return new ResponseEntity<>(
                new HttpResponse(httpStatus.value(), new Date(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(), message), httpStatus);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        userResourceAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}
