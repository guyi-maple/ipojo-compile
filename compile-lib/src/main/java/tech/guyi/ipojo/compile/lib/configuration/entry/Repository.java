package tech.guyi.ipojo.compile.lib.configuration.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repository {

    private String id;
    private String type = "default";
    private String url;

}
