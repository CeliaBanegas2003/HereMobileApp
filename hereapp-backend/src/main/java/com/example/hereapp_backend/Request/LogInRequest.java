package com.example.hereapp_backend.Request;

public class LogInRequest {
    private String email;
    private String password;

    public LogInRequest() {}

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
