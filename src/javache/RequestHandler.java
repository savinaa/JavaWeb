package javache;

import javache.http.*;
//import db.entity.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RequestHandler {
    private HttpRequest httpRequest;

    private HttpResponse httpResponse;

    public HttpSession session;

    RequestHandler(HttpSession session) {
        this.session = session;
    }

    private String pathToDatabase = System.getProperty("user.dir") + "/src/db/users.txt";

    public byte[] handleRequest(String requestContent) {
        this.httpRequest = new HttpRequestImpl(requestContent);
        this.httpResponse = new HttpResponseImpl();

        return identifyUrl(httpRequest.getRequestUrl());
    }

    private byte[] Ok(byte[] content) {
        this.httpResponse.setStatusCode(HttpStatus.OK);
        this.httpResponse.addHeader("Content-Type", "text/html");
        this.httpResponse.setContent(content);
        return httpResponse.getBytes();
    }

    private byte[] NotFound(byte[] content) {
        this.httpResponse.setStatusCode(HttpStatus.NOT_FOUND);
        this.httpResponse.addHeader("Content-Type", "text/html");
        this.httpResponse.setContent("<h1>The requested resource is not found in our database!</h1>".getBytes());
        return httpResponse.getBytes();
    }

    private byte[] BadRequest(byte[] content) {
        httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);
        httpResponse.addHeader("Content-Type", "text/html");
        httpResponse.setContent(content);
        return httpResponse.getBytes();
    }

    private byte[] Redirect(byte[] content) {
        this.httpResponse.setStatusCode(HttpStatus.SEE_OTHER);
        this.httpResponse.setContent(content);
        return this.httpResponse.getBytes();
    }

    public byte[] identifyUrl(String url) {
        String resourcesFolder = System.getProperty("user.dir") + "\\src\\resources";
        String staticResourcesFolder = resourcesFolder + "\\assets";
        String dynamicResourcesFolder = resourcesFolder + "\\pages";

        if ("/login".equals(url)) {
            String email = httpRequest.getBodyParameters().get("email");
            String password = httpRequest.getBodyParameters().get("pwd");

            User existingUser = findUserByEmail(email);
            if (existingUser == null) {
                byte[] pageAnswer = "<h1>Unknown email</h1>".getBytes();
                return BadRequest(pageAnswer);
            }

            if (!existingUser.getPassword().equals(password)) {
                byte[] pageAnswer = "<h1>Uncorrect password!</h1>".getBytes();
                return BadRequest(pageAnswer);
            }
            String sessionId = UUID.randomUUID().toString();

        } else if ("/register".equals(url)) {
            String email = httpRequest.getBodyParameters().get("email");
            String password = httpRequest.getBodyParameters().get("pwd");
            String confirmedPassword = httpRequest.getBodyParameters().get("cpwd");

            User existingUser = findUserByEmail(email);
            if (existingUser != null) {
                return BadRequest("There is already registered user with this email".getBytes());
            }

            if (!password.equals(confirmedPassword)) {
                return BadRequest("Passwords mismatched!".getBytes());
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(pathToDatabase))) {
                bw.write(UUID.randomUUID().toString() + "|" + email + "|" + password + System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpResponse.addHeader("Location", "resources/assets/html/login.html");
            return Redirect(new byte[0]);


        } else if ("/index".equals(url) || "/".equals(url)) {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(staticResourcesFolder + "\\html\\index.html"));
                return Ok(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("/register.html".equals(url)) {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(staticResourcesFolder + "\\html\\register.html"));
                return Ok(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if ("/css/bootstrap.min.css".equals(url)) {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(staticResourcesFolder + "\\css\\bootstrap.min.css"));
                this.httpResponse.setStatusCode(HttpStatus.OK);
                this.httpResponse.addHeader("Content-Type", "text/css");
                this.httpResponse.setContent(fileContent);
                return httpResponse.getBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return NotFound("<h1>Requested url is not found!</h1>".getBytes());
    }

    private User findUser(String property, int indexOfProperty) {
        try (BufferedReader bf = new BufferedReader(new FileReader(pathToDatabase))) {
            String line = bf.readLine();
            while (line != null) {
                String[] userData = line.split("\\|");
                if (userData[indexOfProperty].equals(property)) {
                    User user = new User(userData[0], userData[1], userData[2]);
                    return user;
                }
                line = bf.readLine();
            }
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private User findUserByEmail(String email) {
        return findUser(email, 1);
    }


}
