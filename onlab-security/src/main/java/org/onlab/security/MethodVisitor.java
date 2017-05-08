package org.onlab.security;

/**
 * Created by sdn on 16. 2. 13.
 */

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.EmptyVisitor;
import org.apache.bcel.generic.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Simple method visitor to analyze what APIs are used from given ONOS application.
 * Class copied with modifications from java-callgraph: https://github.com/gousiosg/java-callgraph
 */
public class MethodVisitor extends EmptyVisitor {

    JavaClass visitedClass;
    private MethodGen methodGen;
    private ConstantPoolGen constantPoolGen;
    private PermissionComparer permissionComparer;
    public ArrayList<String> permSet;
    private String format; //
    private String givenType;
    private String permission;

    public MethodVisitor(MethodGen m, JavaClass jc, ArrayList appPermSet, String type) {
        permissionComparer = new PermissionComparer();
        visitedClass = jc;
        methodGen = m;
        constantPoolGen = methodGen.getConstantPool();
        permSet = appPermSet;
        format = "M:" + visitedClass.getClassName() + ":" + methodGen.getName()
                + " " + "(%s)%s:%s"; //
        givenType = type;
    }

    /**
     * Method to start exploration method of given application.
     */
    public void visit() {
        if (methodGen.isAbstract() || methodGen.isNative()) {
            return;
        }
        for (InstructionHandle ih = methodGen.getInstructionList().getStart();
             ih != null; ih = ih.getNext()) {
            Instruction i = ih.getInstruction();
            if (!visitInstruction(i)) {
                i.accept(this);
            }
        }
    }

    /**
     * Method to visit the instruction of given application.
     */
    private boolean visitInstruction(Instruction i) {
        short opcode = i.getOpcode();
        if (givenType.equals("core") && i instanceof GETSTATIC) {
            GETSTATIC st = (GETSTATIC) i;
            permission = st.getFieldName(constantPoolGen);
        }

        return ((InstructionConstants.INSTRUCTIONS[opcode] != null)
                && !(i instanceof ConstantPushInstruction)
                && !(i instanceof ReturnInstruction));
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
        if (givenType.equals("core")) {
            classPermissionMapper((Object) i);
        }
        else if (givenType.equals("app-impl")) {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            String perm =new String();
            if (i.getReferenceType(constantPoolGen).toString().contains("org.onosproject")) {
                List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
                boolean flag = false;
                for( OnosApiMapper oam : oams) {
                    if (visited.equals(oam.interfaceName)) {
                        flag = true;
                        perm = oam.permission;
                        break;
                    }
                }

                if (flag == false) {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    boolean appFlag = false;
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if(!tpc.otherServiceList.contains(visited)) {
                                    tpc.putOtherService(visited);
                                }
                            }
                        }
                    }
                }
                else {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if (perm != null && !tpc.permissionList.contains(perm)) {
                                    tpc.putPermission(perm);
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
            boolean flag = false;
            for( OnosApiMapper oam : oams) {
                if (visited.equals(oam.interfaceName)) {
                    flag = true;
                    break;
                }
            }

            if(flag == false && i.getReferenceType(constantPoolGen).toString().endsWith("Service")) {

                ApplicationChainStore acs = ApplicationChainStore.getInstance();
                String caller = new NameBuilder().classNameBuilder(methodGen.getMethod().getReturnType(),visitedClass.getClassName(),methodGen.getMethod().getName(),methodGen.getMethod().getArgumentTypes());
                ApplicationMapper am = new ApplicationMapper(caller, visited);
                acs.putReaminedmethods(am);
            }

            permissionCheck(visited);
        }
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
        if (givenType.equals("core")) {
            classPermissionMapper((Object) i);
        }
        else if (givenType.equals("app-impl")) {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            String perm =new String();
            if (i.getReferenceType(constantPoolGen).toString().contains("org.onosproject")) {
                List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
                boolean flag = false;
                for( OnosApiMapper oam : oams) {
                    if (visited.equals(oam.interfaceName)) {
                        flag = true;
                        perm = oam.permission;
                        break;
                    }
                }

                if (flag == false) {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if(!tpc.otherServiceList.contains(visited)) {
                                    tpc.putOtherService(visited);
                                }
                            }
                        }
                    }
                }
                else {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if (perm != null && !tpc.permissionList.contains(perm)) {
                                    tpc.putPermission(perm);
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
            boolean flag = false;
            for( OnosApiMapper oam : oams) {
                if (visited.equals(oam.interfaceName)) {
                    flag = true;
                    break;
                }
            }

            if(flag == false && i.getReferenceType(constantPoolGen).toString().endsWith("Service")) {

                ApplicationChainStore acs = ApplicationChainStore.getInstance();
                String caller = new NameBuilder().classNameBuilder(methodGen.getMethod().getReturnType(),visitedClass.getClassName(),methodGen.getMethod().getName(),methodGen.getMethod().getArgumentTypes());
                ApplicationMapper am = new ApplicationMapper(caller, visited);
                acs.putReaminedmethods(am);
            }

            permissionCheck(visited);
        }
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
        if (givenType.equals("core")) {
            classPermissionMapper((Object) i);
        }
        else if (givenType.equals("app-impl")) {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            String perm =new String();
            if (i.getReferenceType(constantPoolGen).toString().contains("org.onosproject")) {
                List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
                boolean flag = false;
                for( OnosApiMapper oam : oams) {
                    if (visited.equals(oam.interfaceName)) {
                        flag = true;
                        perm = oam.permission;
                        break;
                    }
                }

                if (flag == false) {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if(!tpc.otherServiceList.contains(visited)) {
                                    tpc.putOtherService(visited);
                                }
                            }
                        }
                    }
                }
                else {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if (perm != null && !tpc.permissionList.contains(perm)) {
                                    tpc.putPermission(perm);
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
            boolean flag = false;
            for( OnosApiMapper oam : oams) {
                if (visited.equals(oam.interfaceName)) {
                    flag = true;
                    break;
                }
            }

            if(flag == false && i.getReferenceType(constantPoolGen).toString().endsWith("Service")) {

                ApplicationChainStore acs = ApplicationChainStore.getInstance();
                String caller = new NameBuilder().classNameBuilder(methodGen.getMethod().getReturnType(),visitedClass.getClassName(),methodGen.getMethod().getName(),methodGen.getMethod().getArgumentTypes());
                ApplicationMapper am = new ApplicationMapper(caller, visited);
                acs.putReaminedmethods(am);
            }

            permissionCheck(visited);
        }
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        if (givenType.equals("core")) {
                classPermissionMapper((Object) i);
            }
        else if (givenType.equals("app-impl")) {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            String perm =new String();
            if (i.getReferenceType(constantPoolGen).toString().contains("org.onosproject")) {
                List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
                boolean flag = false;
                for( OnosApiMapper oam : oams) {
                    if (visited.equals(oam.interfaceName)) {
                        flag = true;
                        perm = oam.permission;
                        break;
                    }
                }

                if (flag == false) {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if(!tpc.otherServiceList.contains(visited)) {
                                    tpc.putOtherService(visited);
                                }
                            }
                        }
                    }
                }
                else {
                    ThirdPartyServices tps = ThirdPartyServices.getInstance();
                    for (int k=0; k<tps.getComponents().size();k++) {
                        ThirdPartyComponent tpc = tps.getComponents().get(k);
                        if(tpc.implementedClass != null) {
                            if (tpc.implementedClass.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                                if (perm != null && !tpc.permissionList.contains(perm)) {
                                    tpc.putPermission(perm);
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            String visited = new NameBuilder().classNameBuilder(i.getReturnType(constantPoolGen), i.getReferenceType(constantPoolGen).toString(), i.getMethodName(constantPoolGen), i.getArgumentTypes(constantPoolGen));
            List<OnosApiMapper> oams = OnosApiStore.getInstance().getMappers();
            boolean flag = false;
            for( OnosApiMapper oam : oams) {
                if (visited.equals(oam.interfaceName)) {
                    flag = true;
                    break;
                }
            }

            if(flag == false && i.getReferenceType(constantPoolGen).toString().endsWith("Service")) {

                ApplicationChainStore acs = ApplicationChainStore.getInstance();
                String caller = new NameBuilder().classNameBuilder(methodGen.getMethod().getReturnType(),visitedClass.getClassName(),methodGen.getMethod().getName(),methodGen.getMethod().getArgumentTypes());

                ApplicationMapper am = new ApplicationMapper(caller, visited);
                acs.putReaminedmethods(am);
            }
            permissionCheck(visited);
        }
    }

    /**
     * Check the permission had been stored to the necessary permission list of given application already.
     *
     * @param invokedClass  current invoked class
     */
    public void permissionCheck(String invokedClass) {
        String key = permissionComparer.check(invokedClass);
        if (!permSet.contains(key) && key != null) {
            permSet.add(key);
        }
    }

    private void classPermissionMapper(Object i) {
        boolean flag = false;

        if (i.toString().contains("invokestatic")) {
            INVOKESTATIC instruction = (INVOKESTATIC) i;
            if (instruction.getMethodName(constantPoolGen).equals("checkPermission")) {
                flag = true;
            }
        } else if (i.toString().contains("invokespecial")) {
            INVOKESPECIAL instruction = (INVOKESPECIAL) i;
            if (instruction.getMethodName(constantPoolGen).equals("checkPermission")) {
                flag = true;
            }
        } else if (i.toString().contains("invokeinterface")) {
            INVOKEINTERFACE instruction = (INVOKEINTERFACE) i;
            if (instruction.getMethodName(constantPoolGen).equals("checkPermission")) {
                flag = true;
            }
        } else if (i.toString().contains("invokevirtual")) {
            INVOKEVIRTUAL instruction = (INVOKEVIRTUAL) i;
            if (instruction.getMethodName(constantPoolGen).equals("checkPermission")) {
                flag = true;
            }
        }

        if (flag) {
            OnosApiStore oas = OnosApiStore.getInstance();
            List<String> permList = oas.getPermList();
            if (!permList.contains(permission)) {
                permList.add(permission);
            }

            List<OnosApiMapper> mappers = oas.getMappers();
            for (int j = 0; j < mappers.size(); j++) {
                if (mappers.get(j).implementedClassName != null) {
                    if (mappers.get(j).implementedClassName.equals(new NameBuilder().classNameBuilder(methodGen.getMethod(), visitedClass))) {
                        if (permission != null) {
                            mappers.get(j).permission = permission;
                        }
                        break;
                    }
                }
            }
            permission = null;
        }
    }
}
