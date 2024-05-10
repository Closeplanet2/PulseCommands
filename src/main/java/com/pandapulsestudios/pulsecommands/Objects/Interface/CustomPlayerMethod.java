package com.pandapulsestudios.pulsecommands.Objects.Interface;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;

public interface CustomPlayerMethod {
    public PlayerCommandError TryAndInvokeMethod(Player player, String[] player_args);
    public <T> T[] convertArray(Object[] array, Class<?> targetClass);
    public List<String> ReturnTabComplete(Player player, String[] args) throws InvocationTargetException, IllegalAccessException;
    public LinkedHashMap<String, String> ReturnHelpMenu();
    public String ReturnClipboard(String commandName);
    public boolean CanPlayerUseCommand(Player player);
}
