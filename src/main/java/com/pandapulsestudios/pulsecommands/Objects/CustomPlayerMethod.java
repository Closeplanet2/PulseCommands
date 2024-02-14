package com.pandapulsestudios.pulsecommands.Objects;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import com.pandapulsestudios.pulsecommands.Enums.TabType;
import com.pandapulsestudios.pulsecommands.Interface.*;
import com.pandapulsestudios.pulsecommands.PlayerCommand;
import com.pandapulsestudios.pulsecommands.PulseCommands;
import com.pandapulsestudios.pulsecommands.SignatureBuilder;
import com.pandapulsestudios.pulsecommands.Static.StaticList;
import com.pandapulsestudios.pulsecore.Data.API.PlayerDataAPI;
import com.pandapulsestudios.pulsecore.Data.API.ServerDataAPI;
import com.pandapulsestudios.pulsecore.Data.API.UUIDDataAPI;
import com.pandapulsestudios.pulsecore.Data.API.VariableAPI;
import com.pandapulsestudios.pulsecore.Java.PluginAPI;
import com.pandapulsestudios.pulsecore.Java.SoftDependPlugins;
import com.pandapulsestudios.pulsecore.Player.Enums.PlayerAction;
import com.pandapulsestudios.pulsecore.Player.PlayerAPI;
import com.pandapulsestudios.pulsecore._External.WorldGuard.WorldGuardAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CustomPlayerMethod {
    private final PlayerCommand playerCommand;
    private final Method method;

    public CustomPlayerMethod(PlayerCommand playerCommand, Method method){
        this.playerCommand = playerCommand;
        this.method = method;
    }

    public PlayerCommandError TryAndInvokeMethod(Player player, String[] player_args){
        if(!CanPlayerUseCommand(player)) return PlayerCommandError.PlayerCommandsLocked;
        if(method.getParameterTypes().length < 1) return PlayerCommandError.FirstMethodParamMustBeUUID;

        var commandSignature = SignatureBuilder.ReturnSignature(method);
        if(player_args.length < commandSignature.size()) return PlayerCommandError.PlayerInputDifferentCommandSing;
        for(var i = 0; i < commandSignature.size(); i++) if(!commandSignature.get(i).equals(player_args[i])) return PlayerCommandError.PlayerInputDifferentCommandSing;

        var serialisedPlayerInput = new ArrayList<String>(Arrays.asList(player_args).subList(commandSignature.size(), player_args.length));
        var methodParameterTypes = method.getParameterTypes();
        var isLastParamArray = methodParameterTypes.length > 1 && methodParameterTypes[methodParameterTypes.length - 1].isArray();

        if(!isLastParamArray && serialisedPlayerInput.size() + 1 != methodParameterTypes.length) return PlayerCommandError.CommandInvokedWithWrongParams;
        if(isLastParamArray && serialisedPlayerInput.size() + 1 < methodParameterTypes.length) return PlayerCommandError.CommandInvokedWithWrongParams;

        var invokeArgs = new ArrayList<Object>(List.of(player.getUniqueId()));

        for(var i = 1; i < methodParameterTypes.length; i++){
            if(!isLastParamArray){
                var methodParamType = methodParameterTypes[i];
                var playerArg = serialisedPlayerInput.get(i - 1);
                var paramTest = VariableAPI.RETURN_TEST_FROM_TYPE(methodParamType);
                if(paramTest == null) return PlayerCommandError.CommandCannotSerialiseThisData;
                invokeArgs.add(paramTest.DeSerializeData(playerArg));
            }else if(i == methodParameterTypes.length - 1){
                var data = new ArrayList<String>(serialisedPlayerInput.subList(methodParameterTypes.length - 2, serialisedPlayerInput.size()));
                invokeArgs.add(data.toArray(new String[0]));
            }
        }

        try {
            method.invoke(playerCommand, invokeArgs.toArray(new Object[0]));
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return PlayerCommandError.FirstMethodParamMustBeUUID;
        }
    }

    public List<String> ReturnTabComplete(Player player, String[] args) throws Exception {
        var data = new ArrayList<String>();

        if(!CanPlayerUseCommand(player)) return data;

        var canShowFunctionInTab = !method.isAnnotationPresent(PCHideTab.class);
        if(method.isAnnotationPresent(PCFunctionHideTab.class) && canShowFunctionInTab){
            var classMethod = playerCommand.ReturnMethodByName(method.getAnnotation(PCFunctionHideTab.class).value());
            canShowFunctionInTab = (boolean) classMethod.invoke(playerCommand, player.getUniqueId());
        }
        if(!canShowFunctionInTab) return data;

        var player_args = SignatureBuilder.ReturnLivePlayerArguments(args);

        if(method.isAnnotationPresent(PCSignature.class)){
            var sig_list = new LinkedList<String>(Arrays.asList(method.getAnnotation(PCSignature.class).value().split("\\.")));
            sig_list.remove("");
            if(StaticList.B_CONTAINS(player_args, sig_list, args.length) && args.length - 1 < sig_list.size()) data.add(sig_list.get(args.length - 1));
        }

        var methodCommandSignature = SignatureBuilder.ReturnSignature(method);
        if(player_args.size() < methodCommandSignature.size()) return data;
        for(var i = 0; i < methodCommandSignature.size(); i++) if(!methodCommandSignature.get(i).equals(player_args.get(i))) return data;

        var serialisedPlayerInput = new ArrayList<String>();
        for(var i = methodCommandSignature.size(); i < player_args.size(); i++) serialisedPlayerInput.add(player_args.get(i));

        var methodParameterTypes = method.getParameterTypes();
        var isLastParamArray = methodParameterTypes.length > 1 && methodParameterTypes[methodParameterTypes.length - 1].isArray();

        if(!isLastParamArray && serialisedPlayerInput.size() + 1 > methodParameterTypes.length - 1) return data;
        if(isLastParamArray && serialisedPlayerInput.size() + 1 < methodParameterTypes.length - 1) return data;

        var param_test_location = player_args.size() - methodCommandSignature.size();

        if(method.isAnnotationPresent(PCTabs.class)){
            for(var tab : method.getAnnotation(PCTabs.class).value()){
                if(tab.pos() == param_test_location) data.addAll(ConvertTabToList(tab, player_args, player, param_test_location, methodParameterTypes.length, args[args.length - 1]));
                if(isLastParamArray && tab.pos() + 1 == methodParameterTypes.length) data.addAll(ConvertTabToList(tab, player_args, player, 0, methodParameterTypes.length, args[args.length - 1]));
            }
        }else if(method.isAnnotationPresent(PCTab.class)){
            var tab = method.getAnnotation(PCTab.class);
            if(tab.pos() == param_test_location) data.addAll(ConvertTabToList(tab, player_args, player, param_test_location, methodParameterTypes.length, args[args.length - 1]));
            if(isLastParamArray && tab.pos() + 1 == methodParameterTypes.length) data.addAll(ConvertTabToList(tab, player_args, player, 0, methodParameterTypes.length, args[args.length - 1]));
        }

        return data;
    }

    private List<String> ConvertTabToList(PCTab tab, List<String> args, Player player, int pos, int numberOfArguments, String current_input) throws Exception {
        var data = new ArrayList<String>();
        if(pos > numberOfArguments) return data;

        if(tab.type() == TabType.Mixed_Player_Names){
            for(var p : Bukkit.getOnlinePlayers()) data.add(p.getName());
            for(var p : Bukkit.getOfflinePlayers()) data.add(p.getName());
            return data;
        } else if(tab.type() == TabType.Online_Player_Names ){
            for(var p : Bukkit.getOnlinePlayers()) data.add(p.getName());
            return data;
        } else if(tab.type() == TabType.Offline_Player_Names){
            for(var p : Bukkit.getOfflinePlayers()) data.add(p.getName());
            return data;
        } else if(tab.type() == TabType.Information_From_Function){
            var method = playerCommand.ReturnMethodByName(tab.data());
            var methodData = (List<String>) method.invoke(playerCommand, current_input);
            data.addAll(methodData);
            return data;
        } else if(tab.type() == TabType.Stored_Tab_Data_From_Type){
            return data;
        } else if(tab.type() == TabType.Pull_Server_Data){
            var stored_data = ServerDataAPI.GET(tab.data(), new ArrayList<>());
            if(stored_data != null) data.addAll((List<String>) stored_data);
            return data;
        } else if(tab.type() == TabType.Pull_Player_Data){
            var stored_data = UUIDDataAPI.GET(player.getUniqueId(), tab.data(), new ArrayList<>());
            if(stored_data != null) data.addAll((List<String>) stored_data);
            return data;
        } else if(tab.type() == TabType.Pure_Data){
            data.add(tab.data());
            return data;
        }

        return data;
    }

    public boolean CanPlayerUseCommand(Player player){
        if(!PlayerAPI.CanDoAction(PlayerAction.PlayerCommand, player)) return false;
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
}