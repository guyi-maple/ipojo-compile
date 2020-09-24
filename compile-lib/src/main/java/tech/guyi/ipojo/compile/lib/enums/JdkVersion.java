package tech.guyi.ipojo.compile.lib.enums;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JdkVersion {

    @SerializedName("none")
    None(0,0,"Auto"),
    @SerializedName("8")
    JAVA8(8,52,"Java8"),
    @SerializedName("7")
    JAVA7(7,51,"Java7");

    private int value;
    private int target;
    private String text;

}
