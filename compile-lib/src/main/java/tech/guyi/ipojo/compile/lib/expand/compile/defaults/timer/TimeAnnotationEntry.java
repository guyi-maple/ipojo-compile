package tech.guyi.ipojo.compile.lib.expand.compile.defaults.timer;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.guyi.ipojo.compile.lib.utils.AnnotationUtils;

import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
public class TimeAnnotationEntry {

    private String name;
    private int delay;
    private int initDelay;
    private String type;
    private String unit;

    public TimeAnnotationEntry(Annotation annotation){
        this.name = AnnotationUtils.getAnnotationValue(annotation,"name")
                .map(value -> (StringMemberValue) value)
                .map(StringMemberValue::getValue)
                .orElse("");
        this.delay = AnnotationUtils.getAnnotationValue(annotation,"delay")
                .map(value -> (IntegerMemberValue) value)
                .map(IntegerMemberValue::getValue)
                .orElse(0);
        this.initDelay = AnnotationUtils.getAnnotationValue(annotation,"initDelay")
                .map(value -> (IntegerMemberValue) value)
                .map(IntegerMemberValue::getValue)
                .orElse(0);
        this.type = AnnotationUtils.getAnnotationValue(annotation,"type")
                .map(value -> (EnumMemberValue) value)
                .map(EnumMemberValue::getValue)
                .orElse("CYCLE");
        this.unit = AnnotationUtils.getAnnotationValue(annotation,"unit")
                .map(value -> (EnumMemberValue) value)
                .map(EnumMemberValue::getValue)
                .orElse(TimeUnit.MINUTES.toString());
    }

}
