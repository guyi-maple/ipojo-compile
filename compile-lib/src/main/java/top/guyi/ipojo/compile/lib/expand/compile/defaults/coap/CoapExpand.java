package top.guyi.ipojo.compile.lib.expand.compile.defaults.coap;

import javassist.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.*;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.cons.ClassNames;
import top.guyi.ipojo.compile.lib.enums.CompileType;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
class CoapMethodEntry{

    private Annotation mapping;
    private CtClass classes;
    private CtMethod method;
}

/**
 * @author guyi
 * Coap服务扩展
 */
public class CoapExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE && compile.getModules().contains("coap-server");
    }

    private String getName(int index,String[] key){
        if (key.length == 1){
            return key[0];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            sb.append(key[i]).append("_");
        }
        return sb.substring(0,sb.length() - 1);
    }

    private void getResourceString(StringBuilder body,int index,String[] key,CtClass decorator,Set<String> names){
        if (key.length > (index + 1)){
            if (!names.contains(this.getName(index,key))){
                body.append(String.format(
                        "%s %s = new %s(\"%s\");\n",
                        ClassNames.CoapResource,
                        this.getName(index,key),
                        ClassNames.CoapResource,
                        key[index]
                ));
                names.add(this.getName(index,key));

                if (index == 0){
                    body.append(String.format("$1.add(new %s[]{%s});\n\n",
                            ClassNames.Resource,
                            this.getName(index,key)
                    ));
                }else{
                    body.append(String.format(
                            "%s.add((%s)%s);\n",
                            this.getName((index - 1),key),
                            ClassNames.CoapResource,
                            this.getName(index,key)
                    ));
                }
            }
            getResourceString(body,index + 1,key,decorator,names);
        }else{
            names.add(this.getName(index,key));
            body.append(String.format(
                    "%s %s = new %s(\"%s\",$2);\n",
                    ClassNames.CoapResource,
                    this.getName(index,key),
                    decorator.getName(),
                    key[index]
            ));
            body.append(String.format("%s.add((%s)%s);\n\n",
                    this.getName((index - 1),key),
                    ClassNames.CoapResource,
                    this.getName(index,key)
            ));
        }
    }

    /**
     * 获取Coap-Resource注册树
     * @param map
     * @param pool
     * @param components
     * @param compile
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private String getResourceTree(Map<String,List<CoapMethodEntry>> map,ClassPool pool,Set<CompileClass> components,Compile compile) throws CannotCompileException, NotFoundException, IOException, ClassNotFoundException {
        Map<String[],List<CoapMethodEntry>> treeMap = map.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> {
                    if (e.getKey().startsWith("/")){
                        return e.getKey().substring(1).split("/");
                    }else{
                        return e.getKey().split("/");
                    }
                }, Map.Entry::getValue));

        List<String[]> keys = new LinkedList<>(treeMap.keySet());
        keys.sort(Comparator.comparingInt(key -> key.length));

        StringBuilder body = new StringBuilder("{\n");

        Set<String> names = new LinkedHashSet<>();
        for (String[] key : keys) {
            List<CoapMethodEntry> entry = treeMap.get(key);
            CtClass decorator = this.makeCoapHandlerDecorator(entry,pool,components,compile);
            if (key.length > 1){
                this.getResourceString(body,0,key,decorator,names);
            }else{
                names.add(this.getName(0,key));
                body.append(String.format(
                        "%s %s = new %s(\"%s\",$2);\n",
                        ClassNames.CoapResource,
                        key[0],
                        decorator.getName(),
                        key[0]
                ));
                body.append(String.format("$1.add(new %s[]{%s});\n\n",
                        ClassNames.Resource,
                        key[0]
                ));
            }
        }

        body.append("}");

        return body.toString();
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        Map<String,List<CoapMethodEntry>> entryMap = new HashMap<>();
        components
                .stream()
                .map(component ->
                        Optional.ofNullable(component.getClasses().getMethods())
                                .map(Arrays::asList)
                                .orElseGet(LinkedList::new)
                                .stream()
                                .map(method -> AnnotationUtils
                                        .getAnnotation(component.getClasses(),method, AnnotationNames.CoapMapping)
                                        .map(annotation -> new CoapMethodEntry(annotation,component.getClasses(),method))
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .forEach(method -> {
                    AnnotationUtils.getAnnotationValue(method.getMapping(),"path")
                            .map(value -> (StringMemberValue) value)
                            .ifPresent(path -> {
                                List<CoapMethodEntry> list = entryMap.getOrDefault(path.getValue(),new LinkedList<>());
                                list.add(method);
                                entryMap.put(path.getValue(),list);
                            });
                });

        if (!entryMap.isEmpty()) {
            components.add(new CompileClass(pool.get(ClassNames.CoapCurrent)));
            CtClass manager = pool.makeClass(String.format("%s.coap.DefaultCoapServerManager",compile.getPackageName()));
            manager.setSuperclass(pool.get(ClassNames.CoapServerManager));
            compile.addUseComponent(manager);
            CtMethod method = new CtMethod(CtClass.voidType,"registerMapping",new CtClass[]{
                    pool.get(ClassNames.CoapServer),
                    pool.get(ClassNames.ApplicationContext)
            },manager);
            method.setBody(this.getResourceTree(entryMap,pool,components,compile));
            manager.addMethod(method);
            manager.writeFile(compile.getProject().getOutput());
            components.add(new CompileClass(manager,false));
        }
        return components;
    }

    /**
     * 创建Coap-Resource包装器
     * @param methods 目标方法实体列表
     * @param pool
     * @param components
     * @param compile
     * @return
     * @throws NotFoundException
     * @throws CannotCompileException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private CtClass makeCoapHandlerDecorator(List<CoapMethodEntry> methods,ClassPool pool,Set<CompileClass> components,Compile compile) throws NotFoundException, CannotCompileException, ClassNotFoundException, IOException {
        CtClass decorator = pool.makeClass(String.format("%s.coap.decorators.CoapHandlerDecorator%s",compile.getPackageName(),UUID.randomUUID().toString().replaceAll("-","")));
        decorator.setSuperclass(pool.get(ClassNames.CoapHandlerDecorator));

        CtConstructor constructor = new CtConstructor(new CtClass[]{
                pool.get(String.class.getName()),
                pool.get(ClassNames.ApplicationContext)
        },decorator);
        constructor.setBody("{super($$);}");
        decorator.addConstructor(constructor);

        StringBuilder methodBody = new StringBuilder("{\n");
        for (CoapMethodEntry method : methods) {
            CtClass invoker = this.makeCoapInvoker(method,pool,compile);
            String methodName = Optional.ofNullable(method.getMapping().getMemberValue("method"))
                    .map(value -> (EnumMemberValue)  value)
                    .map(EnumMemberValue::getValue)
                    .map(name -> name.substring(name.lastIndexOf(".") + 1))
                    .orElse("POST");
            methodBody.append(String.format(
                    "$0.register(%s.%s,new %s());\n",
                    ClassNames.CoapMethod,
                    methodName,
                    invoker.getName()
            ));
        }
        methodBody.append("}");

        CtMethod method = new CtMethod(CtClass.voidType,"registerAll",new CtClass[0],decorator);
        method.setBody(methodBody.toString());
        decorator.addMethod(method);

        decorator.writeFile(compile.getProject().getOutput());

        components.add(new CompileClass(decorator,false,false,false));

        return decorator;
    }

    /**
     * 创建Coap执行器
     * @param method 目标方法实体
     * @param pool
     * @param compile
     * @return
     * @throws NotFoundException
     * @throws CannotCompileException
     * @throws IOException
     */
    private CtClass makeCoapInvoker(CoapMethodEntry method,ClassPool pool,Compile compile) throws NotFoundException, CannotCompileException, IOException {
        CtClass invoker = pool.makeClass(String.format("%s.coap.invokers.CoapInvoker%s",compile.getPackageName(),UUID.randomUUID().toString().replaceAll("-","")));
        invoker.addInterface(pool.get(ClassNames.CoapResourceInvoker));
        CtClass type = method.getMethod().getParameterTypes()[0];

        CtMethod argsClassMethod = new CtMethod(pool.get(Class.class.getName()),"argsClass",new CtClass[0],invoker);
        argsClassMethod.setBody(String.format("{return %s.class;}",type.getName()));
        invoker.addMethod(argsClassMethod);

        CtMethod invokeMethod = new CtMethod(pool.get(Object.class.getName()),"invoke",new CtClass[]{
                pool.get(ClassNames.ApplicationContext),
                pool.get(Object.class.getName())
        },invoker);

        StringBuilder body = new StringBuilder("{");
        String code = String.format(
                "((%s)$1.get(%s.class,true)).%s((%s) $2);",
                method.getClasses().getName(),
                method.getClasses().getName(),
                method.getMethod().getName(),
                type.getName()
        );
        if (method.getMethod().getReturnType() == CtClass.voidType){
            body.append(code).append("return null;");
        }else{
            body.append("Object result = ").append(code).append("return result;");
        }
        body.append("}");
        invokeMethod.setBody(body.toString());
        invoker.addMethod(invokeMethod);

        invoker.writeFile(compile.getProject().getOutput());

        return invoker;
    }

}
