package com.pandapulsestudios.pulsecommands.Objects;

import com.pandapulsestudios.pulsecommands.Enums.PlayerCommandError;
import com.pandapulsestudios.pulsecommands.Enums.TabType;
import com.pandapulsestudios.pulsecommands.Interface.*;
import com.pandapulsestudios.pulsecommands.PlayerCommand;
import com.pandapulsestudios.pulsecommands.SignatureBuilder;
import com.pandapulsestudios.pulsecore.Player.PlayerAPI;
import com.pandapulsestudios.pulsecore.Player.ToggleActions;
import com.pandapulsestudios.pulsecore.StoredData.PlayerDataAPI;
import com.pandapulsestudios.pulsecore.StoredData.ServerDataAPI;
import com.pandapulsestudios.pulsevariable.API.PulseVarAPI;
import com.pandapulsestudios.pulsevariable.PulseVariable;
import com.pandapulsestudios.pulsevariable.VAR_TESTS.STATIC_TESTS.StaticList;
import com.pandapulsestudios.pulsevariable.VAR_TESTS.STATIC_TESTS.StaticString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CustomPlayerMethod {
    private final PlayerCommand playerCommand;
    private final Method method;
    private final boolean toLower;

    public CustomPlayerMethod(PlayerCommand playerCommand, Method method, boolean toLower){
        this.playerCommand = playerCommand;
        this.method = method;
        this.toLower = toLower;
    }

    public PlayerCommandError TryAndInvokeMethod(Player player, String[] player_args){

        if(!CanPlayerUseCommand(player)){
            return PlayerCommandError.PLAYER_COMMANDS_LOCKED;
        }

        var command_sig = SignatureBuilder.RETURN_SIGNATURE(method);
        var method_param = method.getParameterTypes();

        var is_last_param_array = method_param[method_param.length - 1].isArray();
        if(!(player_args.length == (command_sig.size() + method_param.length - 1)) && !is_last_param_array)
            return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;
        if(!(player_args.length >= (command_sig.size() + method_param.length - 1)) && is_last_param_array)
            return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;

        var expected_types = new ArrayList<String>();
        var expected_data_types = new ArrayList<Class<?>>();
        var invoke_args = new ArrayList<>();
        invoke_args.add(player.getUniqueId());

        for(var x : command_sig){
            expected_types.add(StaticString.CASE(x, false, toLower));
            expected_data_types.add(String.class);
        }

        for(var x : method_param){
            if(x != UUID.class){
                expected_types.add(StaticString.CASE(x.getSimpleName(), false, toLower));
                expected_data_types.add(x);
            }
        }

        //ConsoleAPI.LINE();
        for(var i = 0; i < expected_data_types.size(); i++){
            var name = expected_types.get(i);
            var type = expected_data_types.get(i);
            var player_arg = player_args[i];
            var all_types = PulseVarAPI.RETURN_AS_ALL_TYPES(player_arg, false, toLower, type.isArray());
            if(!all_types.contains(name)) return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;
            var param_test = PulseVariable.VAR_TESTS.get(type);
            if(param_test == null) return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;

            if(i >= command_sig.size()){
                if(!type.isArray()){
                    var converted_arg = param_test.CONVERT_FOR_CONFIG(player_arg);
                    if(converted_arg == null) return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;
                    invoke_args.add(converted_arg);
                }else{
                    var itemsRemoved = new ArrayList<String>(Arrays.asList(player_args).subList(i, player_args.length));
                    param_test.CUSTOM_CAST_AND_PLACE(invoke_args, invoke_args.size() - 1, param_test.CONVERT(itemsRemoved), type);
                }
            }
        }

        try {
            playerCommand.InvokeVoid(method, invoke_args.toArray(new Object[0]));
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return PlayerCommandError.COMMAND_CANNOT_BE_INVOKED_WITH_PROVIDED_ARGUMENTS;
        }
    }

    public List<String> ReturnTabComplete(Player player, String[] args, boolean toHigher, boolean toLower){
        var data = new ArrayList<String>();

        if(!CanPlayerUseCommand(player)){
            data.add(ChatColor.RED + "Cannot use command!");
            return data;
        }

        //PC signature tab complete
        var command_sig = SignatureBuilder.RETURN_SIGNATURE(method);
        var method_param = method.getParameterTypes();
        var lastParam = method_param[method_param.length - 1];

        if(method.isAnnotationPresent(PCSignature.class)){
            var args_list = new LinkedList<String>(Arrays.asList(args));
            args_list.remove("");
            var sig_list = new LinkedList<String>(Arrays.asList(method.getAnnotation(PCSignature.class).value().split("\\.")));
            sig_list.remove("");
            if(StaticList.B_CONTAINS(args_list, sig_list, args.length, toHigher, toLower) && args.length - 1 < sig_list.size())
                data.add(StaticString.CASE(sig_list.get(args.length - 1), toHigher, toLower));
        }

        var param_test_location = args.length - command_sig.size();

        if(method.isAnnotationPresent(PCTabs.class)){
            for(var tab : method.getAnnotation(PCTabs.class).value()){
                if(tab.pos() == param_test_location){
                    data.addAll(ConvertTabToList(tab, args, player, param_test_location, method_param.length));
                }

                if(lastParam.isArray() && tab.pos() + 1 == method_param.length){
                    data.addAll(ConvertTabToList(tab, args, player, 0, method_param.length));
                }
            }
        }else if(method.isAnnotationPresent(PCTab.class)){
            var tab = method.getAnnotation(PCTab.class);
            if(tab.pos() == param_test_location){
                data.addAll(ConvertTabToList(tab, args, player, param_test_location, method_param.length));
            }

            if(lastParam.isArray() && tab.pos() + 1 == method_param.length){
                data.addAll(ConvertTabToList(tab, args, player, 0, method_param.length));
            }
        }

        return data;
    }

    private List<String> ConvertTabToList(PCTab tab, String[] args, Player player, int pos, int numberOfArguments){
        var data = new ArrayList<String>();
        if(pos > numberOfArguments) return data;

        if(tab.type() == TabType.Online_Player_Names || tab.type() == TabType.Mixed_Player_Names){
            for(var p : Bukkit.getOnlinePlayers()) data.add(p.getName());
        }

        if(tab.type() == TabType.Offline_Player_Names || tab.type() == TabType.Mixed_Player_Names){
            for(var p : Bukkit.getOfflinePlayers()) data.add(p.getName());
        }

        if(tab.type() == TabType.Information_From_Function){
            var method = playerCommand.ReturnMethodByName(tab.data());
            if(method == null) return new ArrayList<>();
            try { data.addAll((List<String>) method.invoke(playerCommand));
            } catch (IllegalAccessException | InvocationTargetException e) {
                data.add("No function");
                return data;
            }
        }

        if(tab.type() == TabType.Stored_Tab_Data_From_Type){
            var test = PulseVarAPI.RETURN_TEST_FROM_TYPE(tab.data(), false);
            if(test != null) data.addAll(test.TAB_DATA(new ArrayList<>(), args[args.length - 1]));
        }

        if(tab.type() == TabType.Pull_Server_Data) {
            var stored_data = ServerDataAPI.GET(tab.data());
            if(stored_data != null) data.addAll((List<String>) stored_data);
        }

        if(tab.type() == TabType.Pull_Player_Data){
            var stored_data = PlayerDataAPI.GET(player.getUniqueId(), tab.data());
            if(stored_data != null) data.addAll((List<String>) stored_data);
        }

        return data;
    }

    public boolean CanPlayerUseCommand(Player player){
        if(!PlayerAPI.GET_TOGGLE_STAT(player, ToggleActions.PlayerCommandSendEvent)) return false;
        if(method.isAnnotationPresent(PCOP.class) && !player.isOp()) return false;
        if(method.isAnnotationPresent(PCPerm.class))
            if(!PlayerAPI.TEST_PERMISSIONS(player, method.getAnnotation(PCPerm.class).value())) return false;
        if(method.isAnnotationPresent(PCWorld.class))
            if(!player.getWorld().getName().equals(method.getAnnotation(PCWorld.class).value())) return false;
        return true;

    }
}