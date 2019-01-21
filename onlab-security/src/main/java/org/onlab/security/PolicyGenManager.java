/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.security;

import org.apache.bcel.classfile.ClassParser;
import org.slf4j.Logger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple class to generate app.xml policy file based on given application's necessary permission automatically.
 */
public class PolicyGenManager {

    private final String version = "1.7.0-SNAPSHOT";
    private final Logger log = getLogger(getClass());
    private final String  defaultRepoPath = "/.m2/repository/";
    private String homePath;
    private ArrayList<String> jarPaths;
    private ArrayList<String> appPermSet;
    private String appName;
    private String artifactId;

    public PolicyGenManager(String type) {
        jarPaths = new ArrayList<>();
        homePath = System.getenv("HOME");
        boolean feature = true;
        if(type.equals("core")) {
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-net/1.7.0-SNAPSHOT/onos-core-net-1.7.0-SNAPSHOT.jar"); //
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-common/1.7.0-SNAPSHOT/onos-core-common-1.7.0-SNAPSHOT.jar");
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-dist/1.7.0-SNAPSHOT/onos-core-dist-1.7.0-SNAPSHOT.jar");
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-persistence/1.7.0-SNAPSHOT/onos-core-persistence-1.7.0-SNAPSHOT.jar");
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-primitives/1.7.0-SNAPSHOT/onos-core-primitives-1.7.0-SNAPSHOT.jar");
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-core-serializers/1.7.0-SNAPSHOT/onos-core-serializers-1.7.0-SNAPSHOT.jar");

        }
        else if(type.equals("api")) {
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-api/1.7.0-SNAPSHOT/onos-api-1.7.0-SNAPSHOT.jar");
            jarPaths.add("/home/sdn/.m2/repository/org/onosproject/onos-incubator-api/1.7.0-SNAPSHOT/onos-incubator-api-1.7.0-SNAPSHOT.jar");
        } else {
	    System.out.println("input core or api");
	}
    }
    public PolicyGenManager(Set<String> locations, String name) {
        appName = name;
        appPermSet = new ArrayList<>();
        jarPaths = new ArrayList<>();
        homePath = System.getenv("HOME");
        boolean feature = true;
        String[] bundleLocations = locations.toArray(new String[locations.size()]);

        for (String bundleLocation : bundleLocations) {
            if (feature) {
                artifactId = getArtifactId(bundleLocation);
            }
            jarPaths.add(makeJarpath(bundleLocation));
            feature = false;
        }
        System.out.println("app Name = " + appName);
        for (String location : locations) {
            System.out.println("location : " + location);
        }
        System.out.println("Policy gen");
    }

    /**
     * Method to make the jar path based on the bundle location.
     * @param bundleLocation location of bundle
     * @return location of bundle jar file
     */
    private String makeJarpath(String bundleLocation) {
        String[] parsedLocation = locationParser(bundleLocation);
        String jarPath = "";

        jarPath = jarPath + homePath + defaultRepoPath;
        for (String loc : parsedLocation) {
            jarPath = jarPath + loc + "/";
        }
        jarPath = jarPath + parsedLocation[parsedLocation.length - 2] + "-"
                + parsedLocation[parsedLocation.length - 1] + ".jar";

        return jarPath;
    }

    /**
     * Method to parse the path of bundle location.
     * @param bundleLocation location of bundle
     * @return parsed path
     */
    private String[] locationParser(String bundleLocation) {
        String[] parsedPath = bundleLocation.split("/");
        parsedPath[0] = parsedPath[0].substring(parsedPath[0].indexOf(':') + 1, parsedPath[0].length());
        parsedPath[0] = parsedPath[0].replace('.', '/');

        return parsedPath;
    }

    /**
     * Method to extract the app artifactId from the bundle location.
     * @param bundleLocation location of bundle
     * @return artifactId of app
     */
    private String getArtifactId(String bundleLocation) {
        String[] parsedPath = bundleLocation.split("/");
        String[] sparsedPath = parsedPath[1].split("-");
        return sparsedPath[2];
    }

    private void recursiveRefineAppService(ThirdPartyComponent tpc, List<String> serviceList) {
        ThirdPartyServices tps = ThirdPartyServices.getInstance();
        List<ThirdPartyComponent> tpcs = tps.getComponents();

        for (int i = 0; i < serviceList.size(); i++) {

            for (int j = 0; j < tpcs.size(); j++) {
                ThirdPartyComponent tempTpc = tpcs.get(j);

                if (tempTpc.interfaceClass.equals(serviceList.get(i))) {

                    for (int permNum = 0; permNum < tempTpc.permissionList.size(); permNum++) {
                        String tempPermission = tempTpc.permissionList.get(permNum);

                        if (!tpc.permissionList.contains(tempPermission)) {
                            tpc.putPermission(tempPermission);
                        }
                    }

                    for (int kk = 0; kk < tempTpc.otherServiceList.size(); kk++) {
                        serviceList.add(tempTpc.otherServiceList.get(kk));
                    }
                }
            }
            serviceList.remove(serviceList.get(i));
        }
        if (serviceList.size() != 0) {
            recursiveRefineAppService(tpc, serviceList);
        }
    }


    /**
     * Inspection for given application or ONOS core.
     */
    public void start(String type) {
        ClassParser cp;

        if (type.equals("application")) {
            String appJarPath = "";
            for (String bundlePath : jarPaths) {
                if (bundlePath.contains(artifactId)) {
                    appJarPath = bundlePath;
                }
            }
            jarPaths.remove(appJarPath);
            System.out.println("artifact = " + artifactId + "jarpath = " + appJarPath);
            for (String bundlePath : jarPaths) {
                try {
                    if (bundlePath != null) {
                        File bundleFile = new File(bundlePath);

                        if (!bundleFile.exists()) {
                            log.warn("Jar file " + bundlePath + "does not exist" + "\n");
                        }

                        JarFile bundleJar = new JarFile(bundleFile);
                        Enumeration<JarEntry> bundleEntries = bundleJar.entries();

                        while (bundleEntries.hasMoreElements()) {
                            JarEntry entry = bundleEntries.nextElement();
                            if (entry.isDirectory()) {
                                continue;
                            }

                            if (!entry.getName().endsWith(".class")) {
                                continue;
                            }

                            cp = new ClassParser(bundlePath, entry.getName());
                            ClassVisitor classVisitor = new ClassVisitor(cp.parse(), appPermSet, "app-service");
                            classVisitor.start();
                        }
                    }
                }
                catch (IOException e) {
                    log.warn("Jar file " + appJarPath + "does not exist" + "\n");
                }
            }

            for (String bundlePath : jarPaths) {
                try {
                    if (bundlePath != null) {
                        File bundleFile = new File(bundlePath);

                        if (!bundleFile.exists()) {
                            log.warn("Jar file " + bundlePath + "does not exist" + "\n");
                        }

                        JarFile bundleJar = new JarFile(bundleFile);
                        Enumeration<JarEntry> bundleEntries = bundleJar.entries();

                        while (bundleEntries.hasMoreElements()) {
                            JarEntry entry = bundleEntries.nextElement();
                            if (entry.isDirectory()) {
                                continue;
                            }

                            if (!entry.getName().endsWith(".class")) {
                                continue;
                            }

                            cp = new ClassParser(bundlePath, entry.getName());
                            ClassVisitor classVisitor = new ClassVisitor(cp.parse(), appPermSet, "app-impl");
                            classVisitor.start();
                        }
                    }
                }
                catch (IOException e) {
                    log.warn("Jar file " + appJarPath + "does not exist" + "\n");
                }
            }
            ThirdPartyServices tps = ThirdPartyServices.getInstance();
            List<ThirdPartyComponent> tpcs = tps.getComponents();

            try {
                File file = new File("/home/sdn/app-service-before.txt");
                FileWriter fw = new FileWriter(file, true);
                fw.write("[Application Service List]\n");
                for(int i=0; i< tpcs.size();i++) {
                    ThirdPartyComponent tpc = tpcs.get(i);
                    fw.write("<Interface> : " + tpc.interfaceClass + "<Implemented> = " + tpc.implementedClass +"\n");
                    for(int kk = 0 ; kk < tpc.otherServiceList.size() ; kk++)
                    {
                        fw.write("<<Call Service>> : " + tpc.otherServiceList.get(kk) + "\n");
                    }
                    for(int jj = 0; jj < tpc.permissionList.size() ; jj++) {
                        fw.write("<<permission>> : " + tpc.permissionList.get(jj) + "\n");
                    }
                }
                fw.flush();
                fw.close();
            }
            catch(IOException e) {
            }

            for(int i=0; i< tpcs.size();i++) {
                ThirdPartyComponent tpc = tpcs.get(i);
                for(int kk = 0 ; kk < tpc.otherServiceList.size() ; kk++)
                {
                    List<String> tempList = new ArrayList<String>();
                    tempList.add(tpc.otherServiceList.get(kk));
                    recursiveRefineAppService(tpc,tempList);
                }
            }

                    try {
                    File file = new File("/home/sdn/app-service-after.txt");
                    FileWriter fw = new FileWriter(file, true);
                        fw.write("[Application Service List]\n");
                    for(int i=0; i< tpcs.size();i++) {
                        ThirdPartyComponent tpc = tpcs.get(i);
                        if (tpc.permissionList.size() > 0) {
                            fw.write("<Interface> : " + tpc.interfaceClass + "\n");

                            for (int jj = 0; jj < tpc.permissionList.size(); jj++) {
                                fw.write("<<permission>> : " + tpc.permissionList.get(jj) + "\n");
                            }
                        }
                    }
                    fw.flush();
                    fw.close();
                }
                catch(IOException e) {
                }




            //Round 3
            File bundleFile = new File(appJarPath);
            try {
                JarFile bundleJar = new JarFile(bundleFile);
                Enumeration<JarEntry> bundleEntries = bundleJar.entries();
                while (bundleEntries.hasMoreElements()) {
                    JarEntry entry = bundleEntries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }

                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    cp = new ClassParser(appJarPath, entry.getName());
                    ClassVisitor classVisitor = new ClassVisitor(cp.parse(), appPermSet, type);
                    classVisitor.start();
                }
            }
            catch (IOException e) {
               log.warn("Jar file " + appJarPath + "does not exist" + "\n");
            }
            //Round 2 - find app service

        }
        else {
            try {
                for (String bundlePath : jarPaths) {
                    if (bundlePath != null) {
                        File bundleFile = new File(bundlePath);

                        if (!bundleFile.exists()) {
                            log.warn("Jar file " + bundlePath + "does not exist" + "\n");
                        }

                        JarFile bundleJar = new JarFile(bundleFile);
                        Enumeration<JarEntry> bundleEntries = bundleJar.entries();

                        while (bundleEntries.hasMoreElements()) {
                            JarEntry entry = bundleEntries.nextElement();
                            if (entry.isDirectory()) {
                                continue;
                            }

                            if (!entry.getName().endsWith(".class")) {
                                continue;
                            }

                            cp = new ClassParser(bundlePath, entry.getName());
                            ClassVisitor classVisitor = new ClassVisitor(cp.parse(), appPermSet, type);
                            classVisitor.start();

                        }
                        if (type.equals("application")) {
                            PolicyGenerator xg = new PolicyGenerator();
                            xg.genAppxml(appName, artifactId, appPermSet);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Error while processing bundle jar file : " + e.getMessage());
            }
        }
    }
}
