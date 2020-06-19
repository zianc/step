package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    /*
     * Returns a JSON object with properties specifying whether or not a user
     * is logged in and the corresponding URL to navigate to.
     */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String json = "{";
    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
        json += "\"loggedIn\": \"1\",";
        json += "\"URL\": \"" + userService.createLogoutURL("/index.html") + "\"";
    } else {
        json += "\"loggedIn\": \"0\",";
        json += "\"URL\": \"" + userService.createLoginURL("/index.html") + "\"";
    }
    json += "}";

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }
}
