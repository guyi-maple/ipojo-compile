package top.guyi.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import top.guyi.ipojo.compile.lib.configuration.Compile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class CompileFormat {

    private static final String ValueName = "value";
    private static Gson gson = new Gson();

    private static boolean noSerial(Class<?> type){
        return !String.class.isAssignableFrom(type)
                && !Number.class.isAssignableFrom(type)
                && !Boolean.class.isAssignableFrom(type);
    }

    private static String getFieldName(Method method){
        String name = method.getName().substring(3);
        if (name.length() == 1){
            return name.toLowerCase();
        }
        return name.substring(0,1).toLowerCase() + name.substring(1);
    }

    private static Map<String,Object> getRealValue(Map<String,Object> configuration){
        configuration.forEach((key, value) -> {
            if (value instanceof Map && ((Map) value).containsKey("value")) {
                configuration.put(key, ((Map) value).get("value"));
            }
        });
        return configuration;
    };

    public static Compile format(Map<String,Object> configuration){
        String json = gson.toJson(formatMap(Compile.class,getRealValue(configuration)));
        return gson.fromJson(json,Compile.class);
    }

    private static Map<String,Object> formatMap(Class<?> classes,Map<String,Object> configuration){
        Arrays.stream(classes.getMethods())
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
                        Field field = classes.getDeclaredField(name);
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
                                }else if (Set.class.isAssignableFrom(type)){
                                    value = getSet(value);
                                }else {
                                    value = getObject(type,value);
                                }
                                configuration.put(fieldName,value);
                            });
                });

        return configuration;
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

    private static Set<Object> getSet(Object value){
        Set<Object> set = new HashSet<>();
        if (value instanceof Map){
            if (((Map) value).containsKey(ValueName)){
                Object content = ((Map) value).get(ValueName);
                set = getSet(content);
            }else{
                set.add(value);
            }
            return set;
        }

        if (value instanceof Set){
            set.addAll((Set) value);
            return set;
        }

        if (value instanceof List){
            set.addAll((List) value);
            return set;
        }

        set.add(value);
        return set;
    }

    private static Object getObject(Class<?> type,Object value){
        if (value instanceof Map){
            if (noSerial(type)){
                return formatMap(type,(Map<String, Object>) value);
            }
            if (((Map) value).containsKey(ValueName)){
                Object content = ((Map) value).get(ValueName);
                return getObject(type,content);
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
