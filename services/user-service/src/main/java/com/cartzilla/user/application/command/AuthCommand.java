package com.cartzilla.user.application.command;

public class AuthCommand {
    private AuthCommand() {}

    public record Register(String email, String password, String fullName) {}

    public record Login(String email, String password) {}
}
