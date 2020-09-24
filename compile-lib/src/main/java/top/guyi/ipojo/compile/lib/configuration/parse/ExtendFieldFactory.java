package top.guyi.ipojo.compile.lib.configuration.parse;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExtendFieldFactory {

    private static final String OverrideName = "override";
    private static final String ValueName = "value";

    public static boolean isOverride(Map<String,Object> map){
        return Boolean.parseBoolean(map.getOrDefault(OverrideName,true).toString());
    }

    private static boolean isSerial(Object value){
        return (value instanceof String)
                || (value instanceof Number)
                || (value instanceof Boolean);
    }

    private static Object select(Object source,Object target){
        if (target == null){
            return source;
        }
        if (source == null){
            return target;
        }

        if ((source instanceof Map) && (target instanceof Map)){
            return extendMap((Map<String, Object>) source,(Map<String, Object>) target);
        }
        if ((source instanceof Map) && isSerial(target)){
            return extendMapSerial((Map<String, Object>) source,(Serializable) target);
        }
        if ((source instanceof Map) && (target instanceof List)){
            return extendMapList((Map<String, Object>) source,(List<Object>) target);
        }

        if ((source instanceof List) && (target instanceof List)){
            return extendList((List<Object>)source,(List<Object>) target);
        }
        if ((source instanceof List) && (target instanceof Map)){
            return extendListMap((List<Object>)source,(Map<String, Object>) target);
        }
        if ((source instanceof List) && isSerial(target)){
            return extendListSerial((List<Object>)source,(Serializable) target);
        }

        if (isSerial(source) && (target instanceof List)){
            return extendSerialList((Serializable)source,(List<Object>)target);
        }
        if (isSerial(source) && (target instanceof Map)){
            return extendSerialMap((Serializable)source,(Map<String,Object>)target);
        }
        if (isSerial(source) && (isSerial(target))){
            return extendSerial((Serializable)source,(Serializable)target);
        }

        return target;
    }

    public static Object extend(Object source,Object target){
        return select(source,target);
    }

    public static Object extendMap(Map<String,Object> source,Map<String,Object> target){
        if (!isOverride(target)){
            return target;
        }
        source.forEach((key,value) -> target.put(key,select(value,target.get(key))));
        return target;
    }
    public static Object extendMapList(Map<String,Object> source, List<Object> target){
        Object value = source.get(ValueName);
        source.put(ValueName,select(value,target));
        return source;
    }
    public static Object extendMapSerial(Map<String,Object> source, Serializable target){
        Object value = source.get(ValueName);
        source.put(ValueName,select(value,target));
        return source;
    }


    public static Object extendList(List<Object> source, List<Object> target){
        target.addAll(source);
        return target.stream().distinct().collect(Collectors.toList());
    }
    public static Object extendListSerial(List<Object> source, Serializable target){
        source.add(target);
        return source.stream().distinct().collect(Collectors.toList());
    }
    public static Object extendListMap(List<Object> source, Map<String,Object> target){
        if (!isOverride(target)){
            return target;
        }
        Object value = target.get(ValueName);
        target.put(ValueName,select(source,value));
        return target;
    }


    public static Object extendSerial(Serializable source, Serializable target){
        return target;
    }
    public static Object extendSerialMap(Serializable source, Map<String,Object> target){
        Object value = target.get(ValueName);
        target.put(ValueName,select(source,value));
        return target;
    }
    public static Object extendSerialList(Serializable source, List<Object> target){
        target.add(source);
        return target.stream().distinct().collect(Collectors.toList());
    }

}
