package top.guyi.iot.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CompileFormat {

    private static final String ValueName = "value";
    private static Gson gson = new Gson();

    private static String getFieldName(Method method){
        String name = method.getName().substring(3);
        if (name.length() == 1){
            return name.toLowerCase();
        }
        return name.substring(0,1).toLowerCase() + name.substring(1);
    }

    public static Compile format(Map<String,Object> configuration){
        Arrays.stream(Compile.class.getMethods())
                .filter(method -> method.getName().startsWith("set"))
                .map(method -> {
                    if (method.getParameterCount() != 1){
                        return null;
                    }
                    String name = method.getName().substring(3);
                    if (name.length() == 0){
                        return null;
                    }
                    return method;
                })
                .filter(Objects::nonNull)
                .forEach(method -> {
                    String name = getFieldName(method);
                    try {
                        Field field = Compile.class.getDeclaredField(name);
                        SerializedName serializedName = field.getAnnotation(SerializedName.class);
                        if (serializedName != null){
                            name = serializedName.value();
                        }
                    } catch (NoSuchFieldException e) {}
                    String fieldName = name;
                    Optional.ofNullable(configuration.get(fieldName))
                            .ifPresent(value -> {
                                Class<?> type = method.getParameterTypes()[0];
                                if (Map.class.isAssignableFrom(type)){
                                    value = getMap(value);
                                }else if (List.class.isAssignableFrom(type)){
                                    value = getList(value);
                                }else {
                                    value = getObject(value);
                                }
                                configuration.put(fieldName,value);
                            });
                });
        String json = gson.toJson(configuration);
        return gson.fromJson(json,Compile.class);
    }

    private static Map<String,Object> getMap(Object value){
        Map<String,Object> map = new HashMap<>();
        if (value instanceof Map){
            if (((Map) value).containsKey(ValueName)){
                Object content = ((Map) value).get(ValueName);
                map = getMap(content);
            }else{
                map.putAll((Map<String,Object>) value);
            }
            return map;
        }

        map.put(ValueName,value);
        return map;
    }

    private static List<Object> getList(Object value){
        List<Object> list = new LinkedList<>();
        if (value instanceof Map){
            if (((Map) value).containsKey(ValueName)){
                Object content = ((Map) value).get(ValueName);
                list = getList(content);
            }else{
                list.add(value);
            }
            return list;
        }

        if (value instanceof List){
            list.addAll((List) value);
            return list;
        }

        list.add(value);
        return list;
    }

    private static Object getObject(Object value){
        if (value instanceof Map){
            if (((Map) value).containsKey(ValueName)){
                Object content = ((Map) value).get(ValueName);
                return getObject(content);
            }
            return value;
        }

        if ((value instanceof List)){
            if (((List) value).size() > 0){
                return ((List) value).get(((List) value).size() - 1);
            }else {
                return null;
            }
        }

        return value;
    }

}
