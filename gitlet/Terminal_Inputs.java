package gitlet;

import com.sun.source.tree.Tree;
import jdk.jshell.execution.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.*;

import static gitlet.Utils.*;


public class Terminal_Inputs implements Serializable {

    private static final File cwd = new File(System.getProperty("user.dir"));

    public File global; // .gitlet
    public File blobs; // directory of blobs
    public File commits; // directory of commits that contains nodes
    public File arbolitodirectory; // directory of branches
    public File actualfile; // directory of actual files
    public StagingArea s; // initializing staging area to be able to use it.
    public File activeBranch; // current name of the active branch
    public File stageAreaFile; // global stage area



    public Terminal_Inputs() {
        global = new File(".gitlet");
        blobs = new File(global, "blobs");
        commits = new File(global, "commits");
        arbolitodirectory = new File(global, "branches");
        actualfile = new File(global, "actual_files");
        s = new StagingArea(new TreeMap<>());
        activeBranch = new File(global, "active_branch");
        stageAreaFile = new File(global, "stage_Area");
    }

    public StagingArea getstage() {
        return s;
    }

    public void init() throws IOException {
        if (!(global.exists())) {
            global.mkdir();
            blobs.mkdirs();
            commits.mkdirs();
            arbolitodirectory.mkdir();
            actualfile.mkdir();
            stageAreaFile.mkdirs();
            activeBranch.mkdirs();
            //SHOULD WE INITIALIZE A STAGING AREA AND WRITE INTO THE A STAGE AREA FILE???? we should
            Node n = new Node();
            n.write_to_file(n, commits);

            branchTree master = new branchTree("master", n);
            File branchMaster = Utils.join(arbolitodirectory, master.name);
            Utils.writeObject(branchMaster, master);

            File w = Utils.join(activeBranch, "AB");
            Utils.writeObject(w, master.name);


            //STAGING AREA
            StagingArea current = new StagingArea(n.treeblob);
            File f = Utils.join(stageAreaFile, "SA");
            Utils.writeObject(f, current);



        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
        }
    }


    public void add(String fileName) throws IOException {
        File newfile = new File(fileName);
        boolean the_same = false;


        if (!newfile.exists()) {
            System.out.println("File does not exist.");
            return;
        } else {
            byte[] byteFile = Utils.readContents(newfile);
            Blob current_Blob = new Blob(byteFile);
            File f = Utils.join(stageAreaFile, "SA");
            StagingArea currentStageArea = Utils.readObject(f, StagingArea.class );

            if (currentStageArea.removedBlobs.containsKey(fileName)) {
                currentStageArea.removedBlobs.remove(fileName);
                File t = Utils.join(stageAreaFile, "SA");
                Utils.writeObject(t, currentStageArea);
                return;
            }

            File w = Utils.join(activeBranch, "AB");
            String x = Utils.readObject(w, String.class);
            File currentBranch = Utils.join(arbolitodirectory, x);
            branchTree head = Utils.readObject(currentBranch, branchTree.class);
            Node pointer = head.getP();

            //read all the blobs and see if they adding one already exists with the exact same contents and name
            if (pointer.treeblob.containsKey(fileName)) {
                byte[] v = pointer.treeblob.get(fileName).contents;
                if (java.util.Arrays.equals(v, byteFile)) {
                    currentStageArea.trackingBlobs.put(fileName, pointer.treeblob.get(fileName));
                    return;
                }
            }


            if (!(pointer.treeblob.isEmpty()) && pointer.treeblob.containsKey(fileName) && pointer.treeblob.get(fileName).contents == (current_Blob.contents)) {
                if (currentStageArea.trackingBlobs.containsKey(fileName)){
                    currentStageArea.trackingBlobs.remove(fileName);
                }
                return;
            }else if (currentStageArea.trackingBlobs.containsKey(fileName)) {
                currentStageArea.allBlobs.replace(fileName, current_Blob);
                currentStageArea.trackingBlobs.replace(fileName, current_Blob);
            } else {
                currentStageArea.allBlobs.put(fileName, current_Blob);
                currentStageArea.trackingBlobs.put(fileName, current_Blob);
            }
            File t = Utils.join(stageAreaFile, "SA");
            Utils.writeObject(t, currentStageArea);
        }
    }

    public void commit(String message) {
        File f = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(f, StagingArea.class );
        if (message == "") {
            System.out.println("Please enter a commit message.");

        } else {
            if (currentStageArea.trackingBlobs.isEmpty() && currentStageArea.removedBlobs.isEmpty()) {
                System.out.println("No changes added to the commit.");
            } else {
                File w = Utils.join(activeBranch, "AB");
                String fileName = Utils.readObject(w, String.class);
                File currentBranch = Utils.join(arbolitodirectory, fileName);
                branchTree head = Utils.readObject(currentBranch, branchTree.class);
                Node n = new Node(message, head.p.commit_id, null);

                for (String entry : currentStageArea.trackingBlobs.keySet()) {
                    Blob current_blob = currentStageArea.trackingBlobs.get(entry);
                    File bl = Utils.join(blobs, current_blob.sha);
                    Utils.writeContents(bl, current_blob.contents);
                }

                n.treeblob.putAll(currentStageArea.trackingBlobs);
                currentStageArea.removedBlobs.clear();
                currentStageArea.trackingBlobs.clear();
                currentStageArea.allBlobs.clear();

                //SERIALIZE STAGING AREA
                File t = Utils.join(stageAreaFile, "SA");
                Utils.writeObject(t, currentStageArea);

                //SERIALIZE NODE
                n.write_to_file(n, commits);

                //SERIALIZE HEAD
                head.p = n;
                branchTree z = new branchTree(fileName, head.p);
                File branchMaster = Utils.join(arbolitodirectory, z.getName());
                Utils.writeObject(branchMaster, z);

            }
        }
    }

    public void rm(String filename) {
        File f = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(f, StagingArea.class);


        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree head = Utils.readObject(currentBranch, branchTree.class);
        Node pointer = head.getP();

        if (!(pointer.treeblob.containsKey(filename)) && (!(currentStageArea.trackingBlobs.containsKey(filename)))) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (currentStageArea.trackingBlobs.containsKey(filename) && (!(pointer.treeblob.containsKey(fileName)))) {
            currentStageArea.trackingBlobs.remove(filename);
            Utils.writeObject(f, currentStageArea);
            return;
        }


        if (pointer.parent_id == null) {
            File removed = new File(filename);
            if (removed.exists()) {
                Utils.restrictedDelete(removed);
            }
        }

        if (pointer.treeblob.containsKey(filename)) {
            currentStageArea.removedBlobs.put(filename, pointer.treeblob.get(filename));
            File removed = new File(filename);
            if (removed.exists()) {
                Utils.restrictedDelete(removed);
            }
        }


        // Serializng staging Area.
        Utils.writeObject(f, currentStageArea);

        // Serializing commits
        File g = Utils.join(commits, pointer.commit_id);
        Utils.writeObject(g, pointer);


    }

    public void log() {
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree head = Utils.readObject(currentBranch, branchTree.class);
        Node pointer = head.getP();
        while (pointer.parent_id != null) {
            // return the init commit
            System.out.println("===");
            System.out.println("commit " + pointer.commit_id);
            System.out.println("Date: " + pointer.timeStamp);
            System.out.println(pointer.commit_message);
            System.out.println();
            File thisFile = Utils.join(commits, pointer.parent_id);
            pointer = Utils.readObject(thisFile, Node.class);
        }
        System.out.println("===");
        System.out.println("commit " + pointer.commit_id);
        System.out.println("Date: " + pointer.timeStamp);
        System.out.println(pointer.commit_message);
        System.out.println();
    }

    public void globalLog() {
        List<String> allFiles = Utils.plainFilenamesIn(commits);
        for (String file : allFiles) {
            File thisFile = Utils.join(commits, file);
            Node c = Utils.readObject(thisFile, Node.class);
            System.out.println("===");
            System.out.println("commit " + c.commit_id);
            System.out.println("Date: " + c.timeStamp);
            System.out.println(c.commit_message);
            System.out.println();
        }
    }

    public void find(String commit_message) {
        List<String> allFiles = Utils.plainFilenamesIn(commits);
        boolean exists = false;
        ArrayList<String> all_nodes_with_commit_message = new ArrayList<>();
        for (String file : allFiles) {
            File thisFile = Utils.join(commits, file);
            Node c = Utils.readObject(thisFile, Node.class);
            if (c.commit_message.equals(commit_message)) {
                exists = true;
                all_nodes_with_commit_message.add(c.commit_id);
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
            return;
        } else {
            for (String st : all_nodes_with_commit_message) {
                System.out.println(st);
            }
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        List<String> b = Utils.plainFilenamesIn(arbolitodirectory);
        File w = Utils.join(activeBranch, "AB");
        String c = Utils.readObject(w, String.class);
        for (String name : b) {
            if (name.equals(c)) {
                System.out.println("*" + name);
            } else {
                System.out.println(name);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        File f = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(f, StagingArea.class );
        for (Map.Entry<String, Blob> entry: currentStageArea.trackingBlobs.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (Map.Entry<String, Blob> entry: currentStageArea.removedBlobs.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();

        System.out.println("=== Untracked Files ===");

    }


    public void checkout1(String filename) {
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree head = Utils.readObject(currentBranch, branchTree.class);
        Node pointer = head.getP();
        if (!(pointer.treeblob.containsKey(filename))) {
            System.out.println("File does not exist in that commit.");
        } else {
            Blob blob = pointer.treeblob.get(filename);
            //File to_check = new File(blobs, blob.sha);

            // get the file with same name at the working directory and overwrite if exists, if not create it
            File p = new File(filename);
            if (p.exists()) {
                Utils.writeContents(p, blob.contents);
            } else {
                Utils.writeContents(p, blob.contents);
            }
        }
    }

    public void checkout2(String commitid, String filename) {
        List<String> all_commits_sha = Utils.plainFilenamesIn(commits);
        boolean indicator = false;
        String to_save = "";
        for (String c : all_commits_sha) {
            if (c.equals(commitid)) {
                to_save = c;
                indicator = true;
                break;
            } else if (c.substring(0,commitid.length()).equals(commitid)) {
                to_save = c;
                indicator = true;
                break;
            }
        }
        if (!indicator) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File thisFile = Utils.join(commits, to_save);
        Node this_commit = Utils.readObject(thisFile, Node.class);
        if (!(this_commit.treeblob.containsKey(filename))) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        Blob blob = this_commit.treeblob.get(filename);
        File to_check = new File(blobs, blob.sha);
        Utils.writeContents(new File(filename), blob.contents);
    }

    public void checkout3(String branchName) {
        //gets name of active branch
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);

        // gets the actual branch
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree active = Utils.readObject(currentBranch, branchTree.class);

        //get all the names of all branches
        List<String> b = Utils.plainFilenamesIn(arbolitodirectory);

        boolean branch_indicator = false;
        String to_save = "";
        for (String branch : b) {
            if (branch.equals(branchName)) {
                to_save = branch;
                branch_indicator = true;
            }
        }
        if (!branch_indicator) {
            System.out.println("No such branch exists.");
            return;
        }

        if (active.name.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        File thisFile = Utils.join(arbolitodirectory, to_save);
        branchTree given_branch = Utils.readObject(thisFile, branchTree.class);
        // get the node where the branch to_save is pointing to
        Node given_node = given_branch.getP();

        Node active_node = active.getP();



        File h = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(h, StagingArea.class );
        for (String f : active_node.treeblob.keySet()) {
            File current = new File(f);
            if (current.exists() && ((!(currentStageArea.trackingBlobs.isEmpty())) || (!currentStageArea.removedBlobs.isEmpty()))) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }

        }

        ArrayList<String> blobs_names = new ArrayList<>();

        //Any files that are tracked in the current branch but are not present in the checked-out branch are deleted
        for (String blob_name : active_node.treeblob.keySet()) {
            if (given_node.treeblob.containsKey(blob_name)) {
                Blob blob = given_node.treeblob.get(blob_name);
                Utils.writeContents(new File(blob_name), blob.contents);
            } else {
                blobs_names.add(blob_name);
            }
        }

        for (String key : blobs_names) {
            active_node.treeblob.remove(key);
            Utils.restrictedDelete(key);
        }


        for (String blob_name : given_node.treeblob.keySet()) {
            Blob blob = given_node.treeblob.get(blob_name);
            File to_check = new File(blobs, blob.sha);
            Utils.writeContents(new File(blob_name), blob.contents);
        }


        active.p = given_branch.getP();
        File tz = Utils.join(activeBranch, "AB");
        Utils.writeObject(tz, branchName);

        branchTree z = new branchTree(branchName, active.p);
        File branchMaster = Utils.join(arbolitodirectory, z.getName());
        Utils.writeObject(branchMaster, z);


        currentStageArea.allBlobs.clear();
        currentStageArea.trackingBlobs.clear();
        currentStageArea.removedBlobs.clear();

        File t = Utils.join(stageAreaFile, "SA");
        Utils.writeObject(t, currentStageArea);

        File x = Utils.join(commits, active_node.commit_id);
        Utils.writeObject(x, active_node);
    }


    public void branch(String name) {
        // list of all the branches names
        List<String> allBranchesNames = Utils.plainFilenamesIn(arbolitodirectory);
        if (allBranchesNames.contains(name)) {
            System.out.println("a branch with that name already exists.");
            return;
        }

        //getting the current node
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree head = Utils.readObject(currentBranch, branchTree.class);
        Node headCommits = head.getP();


        // creating the new branch that point to the current node
        branchTree b = new branchTree(name, headCommits);
        File branchMaster = Utils.join(arbolitodirectory, b.getName());
        Utils.writeObject(branchMaster, b);
    }

    public void removeBranch(String branchName) {
        List<String> allBranchesNames = Utils.plainFilenamesIn(arbolitodirectory);
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        if (!(allBranchesNames.contains(branchName))) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        File thisFile = Utils.join(arbolitodirectory, branchName);
        if (branchName.equals(fileName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            thisFile.delete();
        }
    }

    public void reset(String commitID) {

        // checking if commitID exists
        List<String> all_commits_sha = Utils.plainFilenamesIn(commits);
        boolean indicator = false;
        String to_save = "";
        for (String c : all_commits_sha) {
            if (c.equals(commitID)) {
                to_save = c;
                indicator = true;
                break;
            } else if (c.substring(0,commitID.length()).equals(commitID)) {
                to_save = c;
                indicator = true;
                break;
            }
        }
        if (!indicator) {
            System.out.println("No commit with that id exists.");
            return;
        }

        // getting the active node
        File w = Utils.join(activeBranch, "AB");
        String fileName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, fileName);
        branchTree active = Utils.readObject(currentBranch, branchTree.class);
        Node active_node = active.getP();


        //getting the given node
        File thisFile = Utils.join(commits, commitID);
        Node given_node = Utils.readObject(thisFile, Node.class);

        // go through all the files in the current working directory
        String userDirectory = new File("").getAbsolutePath();
        File n = new File(userDirectory);
        String[] a = n.list();



        ArrayList<String> blobs_names = new ArrayList<>();

        //Any files that are tracked in the current branch but are not present in the checked-out branch are deleted
        for (String blob_name : active_node.treeblob.keySet()) {
            if (given_node.treeblob.containsKey(blob_name)) {
                Blob blob = given_node.treeblob.get(blob_name);
                Utils.writeContents(new File(blob_name), blob.contents);
            } else {
                blobs_names.add(blob_name);
            }
        }

        for (String key : blobs_names) {
            active_node.treeblob.remove(key);
            Utils.restrictedDelete(key);
        }


        active.p = given_node;
        File tz = Utils.join(activeBranch, "AB");
        Utils.writeObject(tz, active.name);

        branchTree z = new branchTree(active.name, active.p);
        File branchMaster = Utils.join(arbolitodirectory, z.getName());
        Utils.writeObject(branchMaster, z);

        // cleans stage area
        File f = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(f, StagingArea.class );
        currentStageArea.allBlobs.clear();
        currentStageArea.trackingBlobs.clear();
        currentStageArea.removedBlobs.clear();

        // serialize stage area
        File t = Utils.join(stageAreaFile, "SA");
        Utils.writeObject(t, currentStageArea);
    }

    public void merge(String branchName) throws IOException {
        // get Staging Area
        File f = Utils.join(stageAreaFile, "SA");
        StagingArea currentStageArea = Utils.readObject(f, StagingArea.class );

        // checking if the stage area is clear or not
        if (!(currentStageArea.trackingBlobs.isEmpty()) && (!(currentStageArea.removedBlobs.isEmpty()))) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        // list of all the branches names and outputing error if branchName is not in the directory
        List<String> allBranchesNames = Utils.plainFilenamesIn(arbolitodirectory);
        if (!(allBranchesNames.contains(branchName))) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        //getting the active branch so I can compare if merging with myself
        File w = Utils.join(activeBranch, "AB");
        String activeBranchName = Utils.readObject(w, String.class);
        File currentBranch = Utils.join(arbolitodirectory, activeBranchName);
        branchTree active_branch = Utils.readObject(currentBranch, branchTree.class);
        //getting active_node
        Node active_node = active_branch.getP();

        //getting the node from the given branch
        File branch = Utils.join(arbolitodirectory, activeBranchName);
        branchTree given_branch = Utils.readObject(currentBranch, branchTree.class);
        Node given_node = given_branch.getP();


        //checking if the branchName is the same as the active one
        if (activeBranchName.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        //checking if there are untracked files in the way (do not know how to do this)

        String splitPoint = "done";
        boolean conflict = false;

        if (given_branch != null) {
            splitPoint = split_point(active_branch, given_branch, currentStageArea);
            if (splitPoint.equals("done")) {
                return;
            }
        }

        List<String> all_commits = Utils.plainFilenamesIn(commits);
        Node splitNode = null;
        for (String commit : all_commits) {
            File thisFile = Utils.join(commits, splitPoint);
            splitNode = Utils.readObject(thisFile, Node.class);
        }
        for (String givenNames : given_node.treeblob.keySet()) {
            if (splitNode.treeblob.containsKey(givenNames)) {
                if (!(active_node.treeblob.containsKey(givenNames))) {
                    if (!(splitNode.treeblob.get(givenNames).equals(given_node.treeblob.get(givenNames)))) {
                        mergeConflict(given_branch, active_branch, givenNames);
                        conflict = true;
                    }
                } else if (!(splitNode.treeblob.get(givenNames).equals(given_node.treeblob.get(givenNames)))) {
                    if (active_node.treeblob.get(givenNames).equals(splitNode.treeblob.get(givenNames))) {
                        checkout4(given_branch, givenNames);
                    } else if (!(given_node.treeblob.get(givenNames)).equals(active_node.treeblob.get(givenNames))) {
                        mergeConflict(given_branch, active_branch, givenNames);
                        conflict = true;
                    }
                }
            } else {
                if (!(active_node.treeblob.containsKey(givenNames))) {
                    checkout4(given_branch, givenNames);
                } else if (!(active_node.treeblob.get(givenNames).equals(given_node.treeblob.get(givenNames)))) {
                    mergeConflict(given_branch, active_branch, givenNames);
                    conflict = true;
                }
            }
        }
        for (String splitName : splitNode.treeblob.keySet()) {
            if (!(given_node.treeblob.containsKey(splitName))) {
                if (active_node.treeblob.containsKey(splitName)) {
                    if (active_node.treeblob.get(splitName).equals(splitNode.treeblob.get(splitName))) {
                        rmHelper(splitName, currentStageArea);
                    } else {
                        mergeConflict(given_branch, active_branch, splitName);
                        conflict = true;
                    }
                }
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        } else {
            commit("Merged " + active_branch.name + " with " + branchName + ".");
        }

    }


    private void rmHelper(String splitName, StagingArea currentStage) {
        if (currentStage.trackingBlobs.containsKey(splitName)) {
            Blob b = currentStage.trackingBlobs.get(splitName);
            currentStage.trackingBlobs.remove(splitName);
            currentStage.removedBlobs.put(splitName, b);
        }
        if (currentStage.allBlobs.containsKey(splitName)) {
            currentStage.allBlobs.remove(splitName);
        }
        File removed = new File (splitName);
        if (removed.exists()) {
            removed.delete();
        }
    }

    private void checkout4(branchTree branch, String givenName) throws IOException {
        Blob givenBlob = branch.p.treeblob.get(givenName);
        File f = new File(blobs, givenName);
        File t = new File(f, givenBlob.sha);
        Utils.writeContents(new File(givenName), givenBlob.contents);
        add(givenName);
    }

    private void mergeConflict(branchTree given, branchTree current, String givenName) throws IOException {
        File folder = new File(blobs, givenName);
        File headFile = new File(folder, "idk");
        File givenFile = new File(folder, "ok");
        String shaHead = "empty";
        String shaGiven = "empty";


        if ((current.p.treeblob.containsKey(givenName))) {
            Blob b = current.p.treeblob.get(givenName);
            shaHead = b.sha;
            headFile = new File(folder, shaHead);
        } else {
            headFile.createNewFile();
        }
        if ((given.p.treeblob.containsKey(givenName))) {
            Blob b = given.p.treeblob.get(givenName);
            shaGiven = b.sha;
            givenFile = new File(folder, shaGiven);
        } else {
            givenFile.createNewFile();
        }
        byte[] headByte = Utils.readContents(headFile);
        byte[] givenByte = Utils.readContents(givenFile);
        String headString = new String(headByte);
        String givenString = new String(givenByte);
        String product = "<<<<<<< HEAD\n" + headString + "=======\n" + givenString + ">>>>>>>\n";
        byte[] result  = product.getBytes();
        Utils.writeContents(new File(givenName), result);
        if (shaGiven.equals("empty")) {
            givenFile.delete();
        }
        if (shaHead.equals("empty")) {
            headFile.delete();
        }
    }

    private String split_point(branchTree active, branchTree given, StagingArea currentStage) {
        boolean fastForward = false;
        boolean sameBranch = false;
        boolean ancestor = false;
        int counter = 0;
        String splitPoint = "done";
        Node head_node = active.getP();
        String head_id = head_node.commit_id;

        Node given_node = given.getP();
        String given_id = given_node.commit_id;

        outerloop:
        while (!(splitPoint.equals(given_id))) {
            List<String> all_commits = Utils.plainFilenamesIn(commits);
            for (String commit : all_commits) {
                File thisFile = Utils.join(commits, commit);
                Node c = Utils.readObject(thisFile, Node.class);
                if (c.commit_id.equals(head_id) && counter == 0 && (!(head_id.equals(given_id)))) {
                    fastForward = true;
                    break outerloop;
                } else if (head_id.equals(given_id) && counter == 0) {
                    sameBranch = true;
                    break outerloop;
                } else if (c.commit_id.equals(head_id)) {
                    splitPoint = c.commit_id;
                    break outerloop;
                }
            }
            if (head_id.equals(null)) {
                break;
            }
            counter += 1;
            head_id = head_node.parent_id;
        }

        given = active;
        if (!(splitPoint.equals("done"))) {
            File thisFile = Utils.join(commits, splitPoint);
            Node c = Utils.readObject(thisFile, Node.class);
            Node splitNode = c;

            for (String filos : given_node.treeblob.keySet()) {
                String fileSha = "";
                File filo = new File(filos);
                if (filo.exists()) {
                    fileSha = Utils.sha1(Utils.readContents(filo));
                }
                if (!(head_node.treeblob.containsKey(filos)) && filo.exists() && !(fileSha.equals(given_node.treeblob.containsKey(filos))) &&
                        !(splitNode.treeblob.containsKey(filos)) && !(currentStage.trackingBlobs.containsKey(filos))) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return "done";
                }
            }
        } else if (fastForward) {
            head_node = given_node;
            System.out.println("Current branch fast-forwarded."); //primer cambio !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            checkout3(given.name);
            return "done";
        } else if (sameBranch) {
            System.out.println("Cannot merge a branch with itself.");
            return "done";
        } else if (ancestor) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return "done";
        }
        return splitPoint;
    }

}

