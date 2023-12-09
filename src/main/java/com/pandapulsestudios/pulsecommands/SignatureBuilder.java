package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.Interface.PCSignature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SignatureBuilder {
    public static String BUILD(Method method, String commandName, boolean addUUID, boolean includeCommandSig, boolean includeParams){
        var includeCommandName = commandName != null;
        var sb = new StringBuilder(includeCommandName ? commandName : "");

        if(addUUID){
            if(includeCommandName) sb.append(".");
            sb.append(UUID.class.getSimpleName());
        }

        if(method.isAnnotationPresent(PCSignature.class) && includeCommandSig){
            if(includeCommandName || addUUID) sb.append(".");
            sb.append(method.getAnnotation(PCSignature.class).value());
        }else{
            includeCommandSig = false;
        }

        if(includeParams){
            if(includeCommandSig) sb.append(".");
            for(var i = 0; i < method.getParameterTypes().length; i++){
                //Ignore the first element as it will always be uuid - uuid added above
                if(i == 0) continue;
                var param = method.getParameterTypes()[i];
                sb.append(param.getSimpleName());
                if((i + 1) < method.getParameterTypes().length) sb.append(".");
            }
        }

        return sb.toString();
    }

    public static List<String> RETURN_SIGNATURE(Method method){
        if(!method.isAnnotationPresent(PCSignature.class)) return new ArrayList<>();
        var value = method.getAnnotation(PCSignature.class).value();
        var return_data = new ArrayList<String>();
        if(!value.contains(".")) return_data.add(value);
        else return_data.addAll(Arrays.stream(value.split("\\.")).toList());
        return_data.remove("");
        return return_data;
    }
}