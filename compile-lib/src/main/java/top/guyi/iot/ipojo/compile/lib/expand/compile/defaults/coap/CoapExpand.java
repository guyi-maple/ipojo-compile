package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.coap;

import javassist.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.iot.ipojo.module.coap.CoapHandlerDecorator;
import top.guyi.iot.ipojo.module.coap.CoapResourceInvoker;
import top.guyi.iot.ipojo.module.coap.CoapServerManager;
import top.guyi.iot.ipojo.module.coap.annotation.CoapMapping;
import top.guyi.iot.ipojo.module.coap.enums.CoapMethod;
import top.guyi.iot.ipojo.module.coap.utils.CoapCurrent;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
class CoapMethodEntry{

    @Setter
    private CoapMapping mapping;

    private CtClass classes;
    private CtMethod method;
    public CoapMethodEntry(CtClass classes, CtMethod method) {
        this.classes = classes;
        this.method = method;
    }
}

/**
 * @author guyi
 * Coap服务扩展
 */
public class CoapExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE && compile.getModules().contains("coap");
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
                        CoapResource.class.getName(),
                        this.getName(index,key),
                        CoapResource.class.getName(),
                        key[index]
                ));
                names.add(this.getName(index,key));

                if (index == 0){
                    body.append(String.format("$1.add(new %s[]{%s});\n\n",
                            Resource.class.getName(),
                            this.getName(index,key)
                    ));
                }else{
                    body.append(String.format(
                            "%s.add((%s)%s);\n",
                            this.getName((index - 1),key),
                            CoapResource.class.getName(),
                            this.getName(index,key)
                    ));
                }
            }
            getResourceString(body,index + 1,key,decorator,names);
        }else{
            names.add(this.getName(index,key));
            body.append(String.format(
                    "%s %s = new %s(\"%s\",$2);\n",
                    CoapResource.class.getName(),
                    this.getName(index,key),
                    decorator.getName(),
                    key[index]
            ));
            body.append(String.format("%s.add((%s)%s);\n\n",
                    this.getName((index - 1),key),
                    CoapResource.class.getName(),
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
                .collect(Collectors.toMap(e -> e.getKey().split("/"), Map.Entry::getValue));

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
                        CoapResource.class.getName(),
                        key[0],
                        decorator.getName(),
                        key[0]
                ));
                body.append(String.format("$1.add(new %s[]{%s});\n\n",
                        Resource.class.getName(),
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
                                .filter(method -> method.hasAnnotation(CoapMapping.class))
                                .map(method -> new CoapMethodEntry(component.getClasses(),method))
                                .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .forEach(method -> {
                    try {
                        CoapMapping mapping = (CoapMapping) method.getMethod().getAnnotation(CoapMapping.class);
                        method.setMapping(mapping);
                        List<CoapMethodEntry> list = entryMap.getOrDefault(mapping.path(),new LinkedList<>());
                        list.add(method);
                        entryMap.put(mapping.path(),list);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });

        if (!entryMap.isEmpty()) {
            components.add(new CompileClass(pool.get(CoapCurrent.class.getName())));
            CtClass manager = pool.makeClass(String.format("%s.coap.DefaultCoapServerManager",compile.getPackageName()));
            manager.setSuperclass(pool.get(CoapServerManager.class.getName()));
            CtMethod method = new CtMethod(CtClass.voidType,"registerMapping",new CtClass[]{
                    pool.get(CoapServer.class.getName()),
                    pool.get(ApplicationContext.class.getName())
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
        decorator.setSuperclass(pool.get(CoapHandlerDecorator.class.getName()));

        CtConstructor constructor = new CtConstructor(new CtClass[]{
                pool.get(String.class.getName()),
                pool.get(ApplicationContext.class.getName())
        },decorator);
        constructor.setBody("{super($$);}");
        decorator.addConstructor(constructor);

        StringBuilder methodBody = new StringBuilder("{\n");
        for (CoapMethodEntry method : methods) {
            CtClass invoker = this.makeCoapInvoker(method,pool,compile);
            methodBody.append(String.format(
                    "$0.register(%s.%s,new %s());\n",
                    CoapMethod.class.getName(),
                    method.getMapping().method().getValue(),
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
        invoker.addInterface(pool.get(CoapResourceInvoker.class.getName()));
        CtClass type = method.getMethod().getParameterTypes()[0];

        CtMethod argsClassMethod = new CtMethod(pool.get(Class.class.getName()),"argsClass",new CtClass[0],invoker);
        argsClassMethod.setBody(String.format("{return %s.class;}",type.getName()));
        invoker.addMethod(argsClassMethod);

        CtMethod invokeMethod = new CtMethod(pool.get(Object.class.getName()),"invoke",new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(Object.class.getName())
        },invoker);
        invokeMethod.setBody(String.format(
                "{return ((%s)$1.get(%s.class,true)).%s((%s)$2);}",
                method.getClasses().getName(),
                method.getClasses().getName(),
                method.getMethod().getName(),
                type.getName()));
        invoker.addMethod(invokeMethod);

        invoker.writeFile(compile.getProject().getOutput());

        return invoker;
    }

}
