package com.pandapulsestudios.pulsecommands.Enums;

public enum PlayerCommandError {
    PlayerCommandsLocked("This Player Commands Have Been Locked!"),
    FirstMethodParamMustBeUUID("The first param in a PCMethod must be CRAFTPLAYER.class || UUIID.class"),
    PlayerInputDifferentCommandSing("The input the player has used is different from command signature"),
    CommandInvokedWithWrongParams("The provided arguments don't work with command methods!"),
    CommandCannotSerialiseThisData("Not all vairables in the command can be serialis3ed with the given data! Add a custom variable test for custom data type!"),
    MustBePlayerTOUseCommand("The command sender must be a instance of player to use this command"),
    NoMethodOrCommandFound("No command method that matches the signature");


    public final String error;
    private PlayerCommandError(String error) {
        this.error = error;
    }
}