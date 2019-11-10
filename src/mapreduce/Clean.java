package mapreduce;

import java.util.ArrayList;
import java.util.List;

public class Clean {
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Utils.read_file(args[0]);
        ArrayList<String> remove_folder = new ArrayList<>();
        ArrayList<String> check_remove = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname : hostnames){
            remove_folder.add("ssh root@" + hostname + " rm -rf /tmp/root");
            check_remove.add("ssh root@" + hostname + " ls /tmp/root");
        }

        // Apply health checker
        List<Boolean> returnValue = Utils.launch_actions_with_return(remove_folder);
        System.out.println(returnValue);
        returnValue = Utils.launch_actions_with_return(check_remove);
        System.out.println(returnValue);
    }
}
