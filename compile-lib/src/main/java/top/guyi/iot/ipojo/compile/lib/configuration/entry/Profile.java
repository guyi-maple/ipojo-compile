package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import top.guyi.iot.ipojo.application.utils.StringUtils;

@Data
public class Profile {

    @Expose
    @SerializedName("default")
    private String defaultName = "default";
    private String active;

    public String getProfileName(){
        return StringUtils.isEmpty(this.active) ? this.defaultName : this.active;
    }

}
