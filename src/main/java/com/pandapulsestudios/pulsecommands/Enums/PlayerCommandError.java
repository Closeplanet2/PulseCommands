package com.pandapulsestudios.pulsecommands.Enums;

public enum PlayerCommandError {
    COMMAND_SENDER_NOT_PLAYER("Command sender is not a value of player!"),
    NO_COMMAND_FOUND_FOR_PLAYER_INPUT("Command input does not match any command!"),
    COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS("Command cant be used with provided args!"),
    PLAYER_COMMANDS_LOCKED("This Player Commands Have Been Locked!");

    public final String error;
    private PlayerCommandError(String error) {
        this.error = error;
    }
}