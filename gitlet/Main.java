package gitlet;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */

public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException, ClassNotFoundException {
        Terminal_Inputs terminal = new Terminal_Inputs();
        boolean did_enter = false;

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (args[0].equals("init") && args.length == 1) {
            terminal.init();
            did_enter = true;
        }
        if (!(terminal.global.exists())) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (args[0].equals("add")) {
            terminal.add(args[1]);
            did_enter = true;
        }
        if (args[0].equals("commit")) {
            did_enter = true;
            if (args.length == 1) {
                System.out.println("Please enter a commit message.");
                return;
            } else if ((args[1].equals(""))) {
                System.out.println("Please enter a commit message.");
                return;
            } else if (!(args[1].charAt(0) == '"')) {
                terminal.commit(args[1]);
            } else {
                String[] msg = Arrays.copyOfRange(args, 1, args.length);
                terminal.commit(msg.toString().replaceAll("^\"|\"$", ""));
            }
            return;
        }
        if (args[0].equals("rm")) {
            did_enter = true;
            terminal.rm(args[1]);
            return;
        }
        if (args[0].equals("status")) {
            did_enter = true;
            terminal.status();
            return;
        }
        if (args[0].equals("log")) {
            did_enter = true;
            terminal.log();
            return;
        }
        if (args[0].equals("global-log")) {
            did_enter = true;
            terminal.globalLog();
            return;
        }
        if (args[0].equals("branch")) {
            did_enter = true;
            terminal.branch(args[1]);
            return;
        }
        if (args[0].equals("find")) {
            did_enter = true;
            if (!(args[1].charAt(0) == '"')) {
                terminal.find(args[1]);
            } else {
                String[] msg = Arrays.copyOfRange(args, 1, args.length);
                terminal.find(msg.toString().replaceAll("^\"|\"$", ""));
            }
            return;
        }
        if (args[0].equals("rm-branch")) {
            did_enter = true;
            terminal.removeBranch(args[1]);
            return;
        }
        if (args[0].equals("checkout")) {
            did_enter = true;
            if (args[1].equals("--")) {
                terminal.checkout1(args[2]);
            } else if (args.length > 2 && args[2].equals("--")) {
                terminal.checkout2(args[1], args[3]);
            } else if (args.length == 2) {
                terminal.checkout3(args[1]);
            } else {
                System.out.println("Incorrect operands.");
                return;
            }
            return;
        }
        if (args[0].equals("merge")) {
            did_enter = true;
            terminal.merge(args[1]);
        }
        if (args[0].equals("reset")) {
            did_enter = true;
            terminal.reset(args[1]);
            return;
        }
        if (!did_enter) {
            System.out.println("No command with that name exists.");
        }
    }
}
