package mapreduce;

import java.util.*;

public class Deploy {

    public static Boolean deploy(List<String> hostnames, String host_path, String remote_path) throws InterruptedException {
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> run_copy = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname : hostnames) {
            health_checks.add("ssh -o StrictHostKeyChecking=no root@" + hostname + " hostname");
            create_dirs.add("ssh root@" + hostname + " if test ! -d " + remote_path + "; then mkdir -p " + remote_path + "; fi");
            check_dir.add("ssh root@" + hostname + " ls /tmp/root");
            run_copy.add("scp -r " + host_path + " root@" + hostname + ":" + remote_path);
        }

        // Apply health checker
        List<Boolean> returnValue = Utils.launch_actions_with_return(health_checks);

        // Check all machine are alive and deploy jar
        boolean isNodesOk = returnValue.stream().allMatch(x -> x);
        if (isNodesOk) {
            Utils.launch_actions_without_return(create_dirs);

            // wait for directories creation and check the creation
            Thread.sleep(3000);
            returnValue = Utils.launch_actions_with_return(check_dir);

            // if all directories are created, deploy jar file
            if (returnValue.stream().allMatch(x -> x)) {
                Utils.launch_actions_without_return(run_copy);
            } else {
                System.out.println("Something went wrong during the directories creation");
                return false;
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello from deploy!");
    }
}
