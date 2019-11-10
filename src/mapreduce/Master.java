package mapreduce;

import java.util.*;

public class Master {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Utils.read_file(args[0]);
        Set<String> slaves = new HashSet<>();
        List<String> files = Utils.list_directory(args[1]);
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> run_copy = new ArrayList<>();
        ArrayList<String> run_map = new ArrayList<>();
        ArrayList<String> run_shuffle = new ArrayList<>();
        ArrayList<String> run_reduce = new ArrayList<>();
        ArrayList<String> copy_hostnames_file = new ArrayList<>();
        Random rand = new Random();

        for (String file:files){
            assert hostnames != null;
            String slave = hostnames.get(rand.nextInt(hostnames.size()));
            slaves.add(slave);
            health_checks.add("ssh -o StrictHostKeyChecking=no root@" + slave + " hostname");
            create_dirs.add("ssh root@" + slave + " if test ! -d " + args[2] + "; then mkdir -p " + args[2] + "; fi");
            run_copy.add("scp -r " + file + " root@" + slave + ":" + args[2]);
            check_dir.add("ssh root@" + slave + " ls " + args[2]);
            run_map.add("ssh root@" + slave + " java -jar /tmp/root/job.jar 0 " + file);
            copy_hostnames_file.add("scp " + args[0] + " root@" + slave + ":/tmp/root");

        }
        for (String slave:slaves)
            run_shuffle.add("ssh root@" + slave + " java -jar /tmp/root/job.jar 1 ");

        assert hostnames != null;
        for (String slave:hostnames)
            run_reduce.add("ssh root@" + slave + " java -jar /tmp/root/job.jar 2 ");

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
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
        }

        // Map
        double start_map_time = System.currentTimeMillis();
        Deploy.deploy(hostnames,
                "jar/job.jar", "/tmp/root/");
        Utils.launch_actions_without_return(run_map);
        System.out.println("MAP FINISHED");
        double end_map_time = System.currentTimeMillis();
        double total_map_time = end_map_time - start_map_time;
        System.out.println(String.format("map time: %f", total_map_time/1000));

        // shuffle
        double start_shuffle_time = System.currentTimeMillis();
        Utils.launch_actions_without_return(copy_hostnames_file);
        Utils.launch_actions_without_return(run_shuffle);
        System.out.println("SHUFFLE FINISHED");
        double end_shuffle_time = System.currentTimeMillis();
        double total_shuffle_time = end_shuffle_time - start_shuffle_time;
        System.out.println(String.format("shuffle time: %f", total_shuffle_time/1000));

        // reduce
        double start_reduce_time = System.currentTimeMillis();
        Utils.launch_actions_without_return(run_reduce);
        System.out.println("REDUCE FINISHED");
        double end_reduce_time = System.currentTimeMillis();
        double total_reduce_time = end_reduce_time - start_reduce_time;
        System.out.println(String.format("reduce time: %f", total_reduce_time/1000));
    }
}

