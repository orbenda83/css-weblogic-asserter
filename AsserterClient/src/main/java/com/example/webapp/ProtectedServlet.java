package com.example.webapp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/protected")
public class ProtectedServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String username = request.getRemoteUser();
        
        out.println("<html><body>");
        out.println("<h2>Protected Resource</h2>");
        if (username != null) {
            out.println("<p>Authenticated User: " + username + "</p>");
        } else {
            out.println("<p>No authenticated user found.</p>");
        }
        out.println("</body></html>");
    }
}