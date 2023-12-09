package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.Interface.PCCommand;
import com.pandapulsestudios.pulsecommands.PlayerCommand;
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
    public static void Register(JavaPlugin javaPlugin, String path) throws Exception {
        var full_data = ReturnAllClasses(javaPlugin, path);
        for(var panda_interface : full_data.keySet()){
            if(panda_interface == PCCommand.class) RegisterCommand(full_data.get(panda_interface), javaPlugin);
        }
    }

    private static HashMap<Class<?>, List<Class<?>>> ReturnAllClasses(JavaPlugin javaPlugin, String path) throws URISyntaxException, IOException {
        var information = new HashMap<Class<?>, List<Class<?>>>();
        information.put(PCCommand.class, new ArrayList<>());

        for(var class_name : ReturnClassNames(javaPlugin, path)){
            try {
                var found_class = Class.forName(class_name);
                if(found_class.isAnnotationPresent(PCCommand.class)) information.get(PCCommand.class).add(found_class);
            } catch (ClassNotFoundException e) { e.printStackTrace(); }
        }

        return information;
    }

    private static List<String> ReturnClassNames(JavaPlugin javaPlugin, String path) throws URISyntaxException, IOException{
        var fileDir = new File(javaPlugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        var zip = new ZipInputStream(new FileInputStream(fileDir));
        var classNames = new ArrayList<String>();
        var entry = zip.getNextEntry();
        while(entry != null){
            if(!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().contains("$")){
                var className = entry.getName().replace('/', '.').replace(".class", "");
                if(className.contains(path)){
                    classNames.add(className);
                }
            }
            entry = zip.getNextEntry();
        }
        return classNames;
    }

    private static void RegisterCommand(List<Class<?>> classes, JavaPlugin plugin) throws Exception{
        var commandMap = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
        for(var clazz : classes){
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
    }

    private static Field getField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}