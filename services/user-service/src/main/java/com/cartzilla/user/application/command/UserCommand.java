package com.cartzilla.user.application.command;

public class UserCommand {
    private UserCommand() {}

    public record UpdateProfile(String fullName, String phone) {}
}
