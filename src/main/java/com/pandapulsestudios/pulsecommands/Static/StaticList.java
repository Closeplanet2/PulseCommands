package com.pandapulsestudios.pulsecommands.Static;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StaticList {
    public static boolean B_CONTAINS(List<String> a, List<String> b, int index){
        for(var i = 0; i < Math.min(Math.min(a.size(), b.size()), index); i++) if(!b.get(i).contains(a.get(i))) return false;
        return true;
    }
}
