package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import com.pandapulsestudios.pulsecommands.Interface.PCMethod;
import com.pandapulsestudios.pulsecommands.Interface.PCMethodData;
import com.pandapulsestudios.pulsecommands.Objects.CustomPlayerMethod;
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
    private final boolean toLower;
    private final String commandName;

    public PlayerCommand(String commandName, boolean toLower, String... alias){
        super(commandName.toLowerCase());
        for(var method : this.getClass().getMethods()){
            if(method.isAnnotationPresent(PCMethod.class)) methodArray.add(new CustomPlayerMethod(this, method, toLower));
            if(method.isAnnotationPresent(PCMethodData.class)) liveData.put(method.getName(), method);
        }
        setAliases(Arrays.stream(alias).toList());
        this.toLower = toLower;
        this.commandName = commandName;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] args) {
        if(!(commandSender instanceof Player)) return CommandError(PlayerCommandError.COMMAND_SENDER_NOT_PLAYER, null, commandName);
        var player = (Player) commandSender;
        for(var cm : methodArray){
            var invoke = cm.TryAndInvokeMethod(player, args);
            if(invoke == null) return true;
        }
        return CommandError(PlayerCommandError.NO_COMMAND_FOUND_FOR_PLAYER_INPUT, player, commandName);
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String s, String[] args) throws IllegalArgumentException {
        if(!(commandSender instanceof Player)) new ArrayList<String>();
        var player = (Player) commandSender;
        var data = new ArrayList<String>();
        for(var customMethod : methodArray){ data.addAll(customMethod.ReturnTabComplete(player, args, false, toLower)); }
        return data;
    }

    public Method ReturnMethodByName(String methodName){
        if(!liveData.containsKey(methodName)) return null;
        return liveData.get(methodName);
    }

    public abstract void InvokeVoid(Method method, Object[] invokeArgs) throws InvocationTargetException, IllegalAccessException;
    public abstract boolean CommandError(PlayerCommandError playerCommandError, Player player, String commandName);
}