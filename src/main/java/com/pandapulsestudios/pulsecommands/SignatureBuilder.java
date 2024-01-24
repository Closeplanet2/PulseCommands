package com.pandapulsestudios.pulsecommands;

import com.pandapulsestudios.pulsecommands.Interface.PCSignature;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignatureBuilder {

    public static List<String> ReturnSignature(Method method){
        if(!method.isAnnotationPresent(PCSignature.class)) return new ArrayList<>();
        var value = method.getAnnotation(PCSignature.class).value();
        var return_data = new ArrayList<String>();
        if(!value.contains(".")) return_data.add(value);
        else return_data.addAll(Arrays.stream(value.split("\\.")).toList());
        return_data.remove("");
        return return_data;
    }

    public static List<String> ReturnLivePlayerArguments(String[] args){
        return new ArrayList<String>(Arrays.asList(args).subList(0, args.length - 1));
    }
}