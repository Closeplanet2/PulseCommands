package com.pandapulsestudios.pulsecommands.Objects.Versions;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import com.pandapulsestudios.pulsecommands.Enums.TabType;
import com.pandapulsestudios.pulsecommands.Interface.*;
import com.pandapulsestudios.pulsecommands.Objects.Interface.CustomPlayerMethod;
import com.pandapulsestudios.pulsecommands.PlayerCommand;
import com.pandapulsestudios.pulsecommands.PulseCommands;
import com.pandapulsestudios.pulsecore.JavaAPI.API.PluginAPI;
import com.pandapulsestudios.pulsecore.JavaAPI.Enum.SoftDependPlugins;
import com.pandapulsestudios.pulsecore.PlayerAPI.API.PlayerActionAPI;
import com.pandapulsestudios.pulsecore.PlayerAPI.Enum.PlayerAction;
import com.pandapulsestudios.pulsecore.StorageDataAPI.API.ServerStorageAPI;
import com.pandapulsestudios.pulsecore.StorageDataAPI.API.UUIDStorageAPI;
import com.pandapulsestudios.pulsecore.VariableAPI.API.VariableAPI;
import com.pandapulsestudios.pulsecore.WorldGuard.API.WorldGuardAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R3.CraftOfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CustomPlayerMethod_v1_20_R3 implements CustomPlayerMethod {
    private final PlayerCommand playerCommand;
    private final Method method;

    public CustomPlayerMethod_v1_20_R3(PlayerCommand playerCommand, Method method){
        this.playerCommand = playerCommand;
        this.method = method;
    }

    @Override
    public PlayerCommandError TryAndInvokeMethod(Player player, String[] player_args) {
        if(!CanPlayerUseCommand(player)) return PlayerCommandError.PlayerCommandsLocked;
        if (method.getParameterTypes().length < 1) return PlayerCommandError.FirstMethodParamMustBeUUID;

        var commandSignature = new ArrayList<String>(Arrays.asList(method.getAnnotation(PCSignature.class).value()));
        if (player_args.length < commandSignature.size()) return PlayerCommandError.PlayerInputDifferentCommandSing;

        for (var i = 0; i < commandSignature.size(); i++){
            if (!commandSignature.get(i).equals(player_args[i])) return PlayerCommandError.PlayerInputDifferentCommandSing;
        }

        var serialisedPlayerInput = new ArrayList<String>(Arrays.asList(player_args).subList(commandSignature.size(), player_args.length));
        var methodParameterTypes = method.getParameterTypes();

        var isLastParamArray = methodParameterTypes.length > 1 && methodParameterTypes[methodParameterTypes.length - 1].isArray();
        if(isLastParamArray && serialisedPlayerInput.size() + 1 <= methodParameterTypes.length - 1) return PlayerCommandError.CommandInvokedWithWrongParams;
        if(!isLastParamArray && serialisedPlayerInput.size() != methodParameterTypes.length - 1) return PlayerCommandError.CommandInvokedWithWrongParams;

        var invokeArgs = new ArrayList<Object>(methodParameterTypes[0] == UUID.class ? List.of(player.getUniqueId()) : List.of(player));
        for (var i = 1; i < methodParameterTypes.length; i++) {
            if(!isLastParamArray || i < methodParameterTypes.length - 1){
                invokeArgs.add(SerialiseSingleData(methodParameterTypes[i], serialisedPlayerInput.get(i - 1)));
            }else if(i == methodParameterTypes.length - 1){
                var data = new ArrayList<Object>();
                var startIndex = methodParameterTypes.length - 2;
                var endIndex = serialisedPlayerInput.size();
                for(var playerArgument : new ArrayList<>(serialisedPlayerInput.subList(startIndex, endIndex))) data.add(SerialiseSingleData(methodParameterTypes[i], playerArgument));
                invokeArgs.add(convertArray(data.toArray(), methodParameterTypes[i]));
            }
        }

        for(var i = 0 ; i < method.getParameterTypes().length; i++){
            var parmType = method.getParameterTypes()[i];
            var paramTest = VariableAPI.RETURN_TEST_FROM_TYPE(parmType);
            if(paramTest != null && !paramTest.IsType(invokeArgs.get(i))) return PlayerCommandError.NoMethodOrCommandFound;
        }

        try {
            method.invoke(playerCommand, invokeArgs.toArray(new Object[0]));
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T[] convertArray(Object[] array, Class<?> targetClass) {
        if (array == null || targetClass == null) {
            throw new IllegalArgumentException("Array or target class cannot be null");
        }

        if (array.length == 0) {
            return (T[]) java.lang.reflect.Array.newInstance(targetClass.getComponentType(), 0);
        }

        try {
            T[] convertedArray = (T[]) java.lang.reflect.Array.newInstance(targetClass.getComponentType(), array.length);

            for (int i = 0; i < array.length; i++) {
                convertedArray[i] = (T) targetClass.getComponentType().cast(array[i]);
            }

            return convertedArray;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot cast array element to target class", e);
        }
    }

    @Override
    public List<String> ReturnTabComplete(Player player, String[] args) throws InvocationTargetException, IllegalAccessException {
        if(!CanPlayerUseCommand(player)) return Arrays.asList(new String[]{ChatColor.RED + "[YOU CANNOT USE THIS COMMAND]"});
        var argumentIndex = args.length - 1;
        var data = new ArrayList<String>();

        var canShowFunctionInTab = !method.isAnnotationPresent(PCHideTab.class);
        if(method.isAnnotationPresent(PCFunctionHideTab.class) && canShowFunctionInTab){
            var classMethod = playerCommand.ReturnMethodByName(method.getAnnotation(PCFunctionHideTab.class).value());
            canShowFunctionInTab = (boolean) classMethod.invoke(playerCommand, player.getUniqueId());
        }
        if(!canShowFunctionInTab) return new ArrayList<String>();


        var livePlayerArgs = new ArrayList<String>(Arrays.asList(args).subList(0, argumentIndex));
        var commandSignature = new ArrayList<String>(Arrays.asList(method.getAnnotation(PCSignature.class).value()));

        if(argumentIndex < commandSignature.size()){
            var lastArgumentIndex = argumentIndex - 1;
            if(lastArgumentIndex >= 0){
                var lastPlayerArg = livePlayerArgs.get(lastArgumentIndex);
                var lastSigArg = commandSignature.get(lastArgumentIndex);
                if(!lastPlayerArg.equals(lastSigArg)) return data;
            }
            data.add(commandSignature.get(argumentIndex));
        }

        for(var i = 0; i < commandSignature.size(); i++){
            var commandSig = commandSignature.get(i);
            var playerArg = i < livePlayerArgs.size() ? livePlayerArgs.get(i) : null;
            if(!commandSig.equalsIgnoreCase(playerArg)) return data;
        }

        var storedMethodParameterTypes =  method.getParameterTypes();
        var isLastParamArray = storedMethodParameterTypes[storedMethodParameterTypes.length - 1].isArray();
        var testLocation = args.length - commandSignature.size();
        if(!isLastParamArray && testLocation >= storedMethodParameterTypes.length) return data;
        var parameterType = testLocation >= storedMethodParameterTypes.length ? storedMethodParameterTypes[storedMethodParameterTypes.length - 1] : storedMethodParameterTypes[testLocation];

        if(method.isAnnotationPresent(PCAutoTabs.class)){
            for(var pcAutoTab : method.getAnnotation(PCAutoTabs.class).value()) data.addAll(ConvertPCAutoTab(pcAutoTab, testLocation, parameterType, storedMethodParameterTypes.length, args[args.length - 1]));
        }else if(method.isAnnotationPresent(PCAutoTab.class)){
            data.addAll(ConvertPCAutoTab(method.getAnnotation(PCAutoTab.class), testLocation, parameterType, storedMethodParameterTypes.length, args[args.length - 1]));
        }

        if(method.isAnnotationPresent(PCTabs.class)){
            for(var pcTab : method.getAnnotation(PCTabs.class).value()) data.addAll(ConvertPCTab(player, pcTab, testLocation, storedMethodParameterTypes.length, args[args.length - 1]));
        }else if(method.isAnnotationPresent(PCTab.class)){
            data.addAll(ConvertPCTab(player, method.getAnnotation(PCTab.class), testLocation, storedMethodParameterTypes.length, args[args.length - 1]));
        }

        return data;
    }

    @Override
    public LinkedHashMap<String, String> ReturnHelpMenu() {
        var data = new LinkedHashMap<String, String>();
        if(method.isAnnotationPresent(PCSignature.class)) for(var sig : method.getAnnotation(PCSignature.class).value()) data.put(sig, sig);
        for(var i = 1; i < method.getParameterTypes().length; i++){
            var param = method.getParameterTypes()[i];
            data.put(param.getSimpleName(), param.getSimpleName());
        }
        return data;
    }

    @Override
    public String ReturnClipboard(String commandName) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(commandName);
        if(method.isAnnotationPresent(PCSignature.class)) for(var sig : method.getAnnotation(PCSignature.class).value()) stringBuilder.append(" ").append(sig);
        return stringBuilder.toString();
    }

    @Override
    public boolean CanPlayerUseCommand(Player player) {
        if(!PlayerActionAPI.CanPlayerAction(PlayerAction.PlayerCommand, player.getUniqueId())) return false;
        if(method.isAnnotationPresent(PCOP.class) && !player.isOp()) return false;

        if(method.isAnnotationPresent(PCPerm.class)){
            var values = method.getAnnotation(PCPerm.class).value();
            for(var perm : values){
                if(!player.hasPermission(perm)) return false;
            }
        }

        if(method.isAnnotationPresent(PCWorld.class)){
            var values = method.getAnnotation(PCWorld.class).value();
            if(values.length > 0){
                if(!Arrays.stream(values).toList().contains(player.getWorld().getName())) return false;
            }
        }

        if(method.isAnnotationPresent(PCRegion.class) && PluginAPI.IsPluginInstalled(PulseCommands.Instance, SoftDependPlugins.WorldGuard)){
            var values = method.getAnnotation(PCRegion.class).value();
            for(var region : values){
                if(!WorldGuardAPI.REGION.IsLocationInRegion(player.getWorld(), region, player.getLocation())) return false;
            }
        }

        return true;
    }


    private Object SerialiseSingleData(Class<?> methodParamType, String playerArgument){
        if(methodParamType == Player.class || methodParamType == CraftPlayer.class){
            if(VariableAPI.RETURN_TEST_FROM_TYPE(UUID.class).IsType(playerArgument)) return Bukkit.getPlayer(UUID.fromString(playerArgument));
            else return Bukkit.getPlayer(playerArgument);
        }else if(methodParamType == OfflinePlayer.class || methodParamType == CraftOfflinePlayer.class){
            if(VariableAPI.RETURN_TEST_FROM_TYPE(UUID.class).IsType(playerArgument)) return Bukkit.getOfflinePlayer(UUID.fromString(playerArgument));
            else return Bukkit.getOfflinePlayer(playerArgument);
        }
        var paramTest = VariableAPI.RETURN_TEST_FROM_TYPE(methodParamType);
        return paramTest == null ? playerArgument : paramTest.DeSerializeData(playerArgument);
    }

    private List<String> ConvertPCAutoTab(PCAutoTab pcAutoTab, int testLocation, Class<?> parameterType, int numberOfArguments, String currentInput){
        testLocation = Math.min(testLocation, numberOfArguments - 1);
        if(testLocation != pcAutoTab.pos()) return new ArrayList<>();
        var variableTest = VariableAPI.RETURN_TEST_FROM_TYPE(parameterType);
        if(variableTest != null) return variableTest.TabData(new ArrayList<>(), currentInput);
        return SubData(parameterType, currentInput);
    }

    private List<String> ConvertPCTab(Player player, PCTab pcTab, int testLocation, int numberOfArguments, String currentInput) throws InvocationTargetException, IllegalAccessException {
        testLocation = Math.min(testLocation, numberOfArguments - 1);
        if(testLocation != pcTab.pos()) return new ArrayList<>();
        if(pcTab.type() == TabType.Information_From_Function){
            var method = playerCommand.ReturnMethodByName(pcTab.data());
            return (List<String>) method.invoke(playerCommand, currentInput);
        }else if(pcTab.type() == TabType.Pull_Server_Data){
            return (List<String>) ServerStorageAPI.Get(pcTab.data(), new ArrayList<String>()).getStorageData();
        }else if(pcTab.type() == TabType.Pull_Player_Data){
            return (List<String>) UUIDStorageAPI.Get(player.getUniqueId(), pcTab.data(), new ArrayList<String>()).getStorageData();
        }else if(pcTab.type() == TabType.Pure_Data){
            return Arrays.asList(new String[]{pcTab.data()});
        }else{
            return new ArrayList<>();
        }
    }

    private List<String> SubData(Class<?> parameterType, String currentInput){
        var data = new ArrayList<String>();
        if(parameterType == OfflinePlayer.class || parameterType == CraftOfflinePlayer.class){
            for(var p : Bukkit.getOfflinePlayers()){
                if(p.getName().toLowerCase().contains(currentInput.toLowerCase())) data.add(p.getUniqueId().toString());
            }
            if(data.isEmpty()) data.add(ChatColor.RED + "[NO PLAYER FOUND]");
        }else if(parameterType == Player.class || parameterType == CraftPlayer.class){
            for(var p : Bukkit.getOnlinePlayers()){
                if(p.getName().toLowerCase().contains(currentInput.toLowerCase())) data.add(p.getUniqueId().toString());
            }
            if(data.isEmpty()) data.add(ChatColor.RED + "[NO PLAYER FOUND]");
        }
        return data;
    }
}