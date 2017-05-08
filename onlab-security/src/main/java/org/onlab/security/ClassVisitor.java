package org.onlab.security;

import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Simple class visitor to invoke the method visitor class.
 * Class copied with modifications from java-callgraph: https://github.com/gousiosg/java-callgraph
 */
public class ClassVisitor extends EmptyVisitor {

    private JavaClass javaClass;
    private ConstantPoolGen constants;
    private ArrayList<String> appPermset;
    private String givenType;

    public ClassVisitor(JavaClass jc, ArrayList<String> store, String type) {
        javaClass = jc;
        constants = new ConstantPoolGen(javaClass.getConstantPool());
        appPermset = store;
        givenType = type;
    }

    /**
     * Implemented method of interface org.apache.bcel.classfile.Visitor.
     */
    public void visitJavaClass(JavaClass jc) {
        jc.getConstantPool().accept(this);
        Method[] methods = jc.getMethods();

        if(givenType.equals("api")) {
            OnosApiStore oas = OnosApiStore.getInstance();
            for (int i = 0; i < methods.length; i++) {
                if(jc.getClassName().endsWith("Service") && !jc.getClassName().contains("ProviderService") && !jc.getClassName().contains("security")) {
                    oas.putMapper(new NameBuilder().interfaceNameBuilder(methods[i],jc), null, null);
                }
            }
        }
        else if (givenType.equals("core")) {
            OnosApiStore oas = OnosApiStore.getInstance();
            for (int i = 0; i < methods.length; i++) {
                String[] interNames = jc.getInterfaceNames();
                for(String interName : interNames) {
                    interName = new NameBuilder().interfaceNameBuilder(methods[i],interName);
                    for (int j=0; j< oas.getMappers().size() ; j++) {
                        OnosApiMapper mapper = oas.getMappers().get(j);
                        if(mapper.interfaceName.equals(interName)) {
                            mapper.implementedClassName = new NameBuilder().classNameBuilder(methods[i],jc);
                            break;
                        }
                    }
                }
                methods[i].accept(this);
            }
        }
        else if(givenType.equals("app-service")) {
            ThirdPartyServices tps = ThirdPartyServices.getInstance();
            for (int i = 0; i < methods.length; i++) {
                if(jc.getClassName().contains("org.onosproject") && !jc.getClassName().contains("$")) {
                    String param = new NameBuilder().interfaceNameBuilder(methods[i],jc);
                    tps.createComponent(param,param);
                }
            }
        }

        else if(givenType.equals("app-impl")) {
            ThirdPartyServices tps = ThirdPartyServices.getInstance();
            for (int i = 0; i < methods.length; i++) {
                String[] interNames = jc.getInterfaceNames();
                for(String interName : interNames) {
                    interName = new NameBuilder().interfaceNameBuilder(methods[i],interName);
                    for (int j=0; j< tps.getComponents().size() ; j++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(j);
                        if(tpc.interfaceClass.equals(interName)) {
                            tpc.implementedClass = new NameBuilder().classNameBuilder(methods[i],jc);
                            break;
                        }
                    }
                }
                methods[i].accept(this);
            }
        }

        else {
            for (int i = 0; i < methods.length; i++) {
                methods[i].accept(this);
            }
        }
    }
    /**
     * Implemented method of interface org.apache.bcel.classfile.Visitor.
     */
    public void visitConstantPool(ConstantPool constantPool) {
        for (int i = 0; i < constantPool.getLength(); i++) {
            Constant constant = constantPool.getConstant(i);
            if (constant == null) {
                continue;
            }
        }
    }
    /**
     * Implemented method of interface org.apache.bcel.classfile.Visitor.
     */
    public void visitMethod(Method method) {
        MethodGen mg = new MethodGen(method, javaClass.getClassName(), constants);
        MethodVisitor visitor = new MethodVisitor(mg, javaClass, appPermset, givenType);
        visitor.visit();
    }

    /**
     * Method to start class exploration.
     */
    public void start() {
        visitJavaClass(javaClass);
    }
}