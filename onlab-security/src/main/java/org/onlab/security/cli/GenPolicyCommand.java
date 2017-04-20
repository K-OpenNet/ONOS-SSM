package org.onlab.security.cli;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.security.*;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages application inventory.
 */
@Command(scope = "onos", name = "genpolicy",
        description = "Generate policy for SM-ONOS automatically")
public class GenPolicyCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Application name",
            required = true, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        print("[Start] Automatic Policy Generation");
        ApplicationAdminService applicationAdminService = get(ApplicationAdminService.class);
        ApplicationId appId = applicationAdminService.getId(name);
        if (appId == null) {
            print("No such application: %s", name);
            return;
        }
        Application app = applicationAdminService.getApplication(appId);
        Set<String> bundleLocations = getBundleLocations(app);
        if (bundleLocations == null) {
            print("Bundle information is not available");
            return;
        } else {
            for (String location : bundleLocations) {
                System.out.println("location" + location);
            }

            if(!(OnosApiStore.getInstance().getMappers().size()>0)) {
                PolicyGenManager apiManager = new PolicyGenManager("api");
                apiManager.start("api");

                PolicyGenManager frameworkManager = new PolicyGenManager("core");
                frameworkManager.start("core");

//                try {
//                    System.out.println("log in!!!!!!!!!!!!!!!!!!!\n");
//                    File file = new File("/home/sdn/Mapper.txt");
//                    FileWriter fw = new FileWriter(file, true);
//                    OnosApiStore oas = OnosApiStore.getInstance();
//                    List<OnosApiMapper> oams = oas.getMappers();
//                    for(int i=0; i< oams.size();i++) {
//                        OnosApiMapper oam = oams.get(i);
//                            fw.write("Interface : " + oam.implementedClassName + "Implemented Class = " + oam.interfaceName + "Permission = " + oam.permission+"\n");
//                        }
//                    fw.flush();
//                    fw.close();
//                }
//                catch(IOException e) {
//
//                }
                refineMappingTable();
            }

//
//            for(int i=0;i<oams.size();i++) {
//                OnosApiMapper mapper = oams.get(i);
//                if(mapper.permission == null) {
//                    System.out.println("Interface : " + mapper.interfaceName + " Class : " + mapper.className + "\n");
//                }
//            }
//            if (oas.getnMappers().size() > 0) {
//                List<OnosApiMapper> noams = oas.getnMappers();
//                System.out.println("[Integrity violation]" + "\n");
//                for (int i = 0; i < noams.size(); i++) {
//                    OnosApiMapper nmapper = noams.get(i);
//                    System.out.println("Interface : " + nmapper.interfaceName + "Class : " + nmapper.implementedClassName + "\n");
//                }
//
//            }

            PolicyGenManager policyManager = new PolicyGenManager(bundleLocations, appId.name());
            policyManager.start("application");
            ApplicationChainStore acs = ApplicationChainStore.getInstance();
            for(ApplicationMapper am : acs.getReaminedmethods()) {
                System.out.println("Method : " + am.calle + "\n");
            }
            ThirdPartyServices.getInstance().resetAppComponents();
        }
    }

    private void refineMappingTable() {
        OnosApiStore oas = OnosApiStore.getInstance();
        List<OnosApiMapper> oams = oas.getMappers();

        System.out.println("[Refine Class]\n");


        for (Iterator<OnosApiMapper> iterator = oams.iterator(); iterator.hasNext();) {
            OnosApiMapper mapper = iterator.next();
            if(mapper.implementedClassName == null) {
                System.out.println("before size = " + oams.size() + "after size = " );
                iterator.remove();
                System.out.println(oams.size()+"\n");
                //oas.removeMapper(oam);
            }
        }
        System.out.println("[Refine permission]\n");
        for (Iterator<OnosApiMapper> iterator = oams.iterator(); iterator.hasNext();) {
            OnosApiMapper mapper = iterator.next();
            if(mapper.permission == null) {
                oas.putnMapper(mapper.interfaceName, mapper.implementedClassName);
                System.out.println("before size = " + oams.size() + "after size = " );
                iterator.remove();
                System.out.println(oams.size()+"\n");
                //oas.removeMapper(oam);
            }
        }
    }
    private Set<String> getBundleLocations(Application app) {
        FeaturesService featuresService = get(FeaturesService.class);
        Set<String> locations = new HashSet<>();
        for (String name : app.features()) {
            try {
                Feature feature = featuresService.getFeature(name);
                locations.addAll(
                        feature.getBundles().stream().map(BundleInfo::getLocation).collect(Collectors.toList()));
            } catch (Exception e) {
                print("[Exception] Fail to find bundle location.");
                return locations;
            }
        }
        return locations;
    }

}