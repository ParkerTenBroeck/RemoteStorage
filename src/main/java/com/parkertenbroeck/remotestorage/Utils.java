package com.parkertenbroeck.remotestorage;


import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class Utils {


    public static <T extends CustomPayload> CustomPayload.Id<T> createId(Class<T> clazz){
        return new CustomPayload.Id<>(createIdentifier(clazz));
    }

    public static <T> Identifier createIdentifier(Class<T> clazz){
        return Identifier.of(RemoteStorage.MOD_ID, toSnakeCase(clazz.getSimpleName()));
    }

    public static String toSnakeCase(String str){
        if(str.length()<2)return str.toLowerCase();

        var ident = new StringBuilder();
        var c = str.charAt(0);
        var n = str.charAt(1);
        for(int i = 2; ; i ++){
            ident.append(Character.toLowerCase(c));
            if(Character.isLowerCase(c)&&Character.isUpperCase(n))ident.append('_');
            if(i >= str.length())break;
            c = n;
            n = str.charAt(i);
        }
        ident.append(Character.toLowerCase(n));

        return ident.toString();
    }
}
