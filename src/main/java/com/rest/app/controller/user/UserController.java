package com.rest.app.controller.user;

import com.rest.app.config.exceptions.ApplicationExceptions;
import com.rest.app.controller.Controller;
import com.rest.app.domain.user.NewUser;
import com.rest.app.domain.user.User;
import com.rest.app.domain.user.UserService;
import com.rest.app.utils.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@RestController
@Component
@Path("/api/users")
public class UserController extends Controller {

    @Inject
    public UserService userService;

    @Override
    @Path("/register")
    public void execute(HttpExchange exchange) throws IOException {
        byte[] response;
        if ("POST".equals(exchange.getRequestMethod())) {
            ResponseEntity<RegistrationResponse> e = doPost(exchange.getRequestBody());
            exchange.getResponseHeaders().putAll(e.getHeaders());
            exchange.sendResponseHeaders(e.getHttpStatusCode().getCode(), 0);
            response = super.writeResponse(e.getBody());
        } else {
            throw ApplicationExceptions.methodNotAllowed(
                    "Method " + exchange.getRequestMethod() + " is not allowed for " + exchange.getRequestURI()).get();
        }

        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    @Path("/list")
    public void list(HttpExchange exchange) throws IOException {
        byte[] response;
        if ("GET".equals(exchange.getRequestMethod())) {
            ResponseEntity<ListUsersResponse> e = doList();
            exchange.getResponseHeaders().putAll(e.getHeaders());
            exchange.sendResponseHeaders(e.getHttpStatusCode().getCode(), 0);
            response = super.writeResponse(e.getBody());
        } else {
            throw ApplicationExceptions.methodNotAllowed(
                    "Method " + exchange.getRequestMethod() + " is not allowed for " + exchange.getRequestURI()).get();
        }

        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private ResponseEntity<RegistrationResponse> doPost(InputStream is) {
        RegistrationRequest registerRequest = super.readRequest(is, RegistrationRequest.class);

        NewUser user = NewUser.builder()
                .login(registerRequest.login())
                .password(PasswordEncoder.encode(registerRequest.password()))
                .build();

        String userId = userService.create(user);

        RegistrationResponse response = new RegistrationResponse(userId);

        return new ResponseEntity<>(response,
                getHeaders(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON), HttpStatusCode.OK);
    }


    private ResponseEntity<ListUsersResponse> doList() {
        List<User> users = userService.list();

        ListUsersResponse response = new ListUsersResponse(users);

        return new ResponseEntity<>(response,
                getHeaders(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON), HttpStatusCode.OK);
    }
}
