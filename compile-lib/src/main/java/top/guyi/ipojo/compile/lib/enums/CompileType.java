package top.guyi.ipojo.compile.lib.enums;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CompileType {

    @SerializedName("bundle")
    BUNDLE("bundle","OSGI-Bundle"),
    @SerializedName("component")
    COMPONENT("component","IPojo-Component");

    private String value;
    private String text;

    public static Optional<CompileType> getByValue(String value){
        return Arrays.stream(values())
                .filter(type -> type.getValue().equals(value))
                .findFirst();
    }

}
