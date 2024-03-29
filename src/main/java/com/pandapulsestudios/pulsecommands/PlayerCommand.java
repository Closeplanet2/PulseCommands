package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import com.pandapulsestudios.pulsecommands.Interface.PCMethod;
import com.pandapulsestudios.pulsecommands.Interface.PCMethodData;
import com.pandapulsestudios.pulsecommands.Objects.CustomPlayerMethod;
import com.pandapulsestudios.pulsecore.Chat.ChatAPI;
import com.pandapulsestudios.pulsecore.Chat.MessageType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class PlayerCommand extends BukkitCommand {

    private final List<CustomPlayerMethod> methodArray = new ArrayList<>();
    private final HashMap<String, Method> liveData = new HashMap<>();
    private final String commandName;
    private final boolean debugErrors;

    public PlayerCommand(String commandName, boolean debugErrors, String... alias){
        super(commandName.toLowerCase());
        for(var method : this.getClass().getMethods()){
            if(method.isAnnotationPresent(PCMethod.class)) methodArray.add(new CustomPlayerMethod(this, method));
            if(method.isAnnotationPresent(PCMethodData.class)) liveData.put(method.getName(), method);
        }
        setAliases(Arrays.stream(alias).toList());
        this.commandName = commandName;
        this.debugErrors = debugErrors;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] args) {
        if(!(commandSender instanceof Player)){
            if(debugErrors) ChatAPI.chatBuilder().SendMessage(PlayerCommandError.MustBePlayerTOUseCommand.error);
            return false;
        }

        var player = (Player) commandSender;
        for(var cm : methodArray){
            var invoke = cm.TryAndInvokeMethod(player, args);
            if(invoke == null) return true;
            else if(debugErrors) ChatAPI.chatBuilder().SendMessage(invoke.error);
        }

        if(debugErrors) ChatAPI.chatBuilder().SendMessage(PlayerCommandError.NoMethodOrCommandFound.error);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String s, String[] args) throws IllegalArgumentException {
        if(!(commandSender instanceof Player)) new ArrayList<String>();
        var player = (Player) commandSender;
        var data = new ArrayList<String>();
        for(var customMethod : methodArray){
            try {data.addAll(customMethod.ReturnTabComplete(player, args));}
            catch ( Exception e) { throw new RuntimeException(e);}
        }
        return data;
    }

    public Method ReturnMethodByName(String methodName){
       return liveData.getOrDefault(methodName, null);
    }

}