package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.PlayerCommand;
import com.pandapulsestudios.pulsecore.Java.JavaAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipInputStream;

//TODO Create console command

public class PulseCommands extends JavaPlugin {

    public static PulseCommands Instance;


    @Override
    public void onEnable() {
        Instance = this;
    }

    public static void RegisterRaw(JavaPlugin javaPlugin) {
        try {
            Register(javaPlugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void Register(JavaPlugin javaPlugin) throws Exception {
        for(var autoRegisterClass : JavaAPI.ReturnAllAutoRegisterClasses(javaPlugin)){
            if(PlayerCommand.class.isAssignableFrom(autoRegisterClass)) RegisterCommand(autoRegisterClass, javaPlugin);
        }
    }

    private static void RegisterCommand(Class<?> clazz, JavaPlugin plugin) throws Exception{
        var commandMap = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
        var commandClass = (Class<? extends PlayerCommand>) clazz;
        PlayerCommand command;
        try {
            command = commandClass.getConstructor(plugin.getClass()).newInstance(plugin);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            try {
                command = commandClass.getConstructor(JavaPlugin.class).newInstance(plugin);
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                try {
                    command = commandClass.getConstructor().newInstance();
                } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex2) {
                    ex2.printStackTrace();
                    return;
                }
            }
        }

        commandMap.register(plugin.getName(), command);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + plugin.getDescription().getFullName() + ": Registered command " + command.getName());
    }

    private static Field getField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}