package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TreeMap;
import static gitlet.Utils.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Node implements Serializable {
    String commit_id;
    String parent_id; //this makes commit tree obsolete.
    String commit_message;
    String timeStamp;
    String  mergeParentID;
    /**
     * hashblob String filename, String commit_id (sha value)
     */
    TreeMap<String, Blob> treeblob; //treemap of blobs in each commit. each commit has a treeblob instance
    Terminal_Inputs current = new Terminal_Inputs(); //Initializing to be able to use terminal methods
    StagingArea stage = current.getstage(); //Initializing to be able to use staging area methods.


    /**
     * initializing first commit (when doing init).
     */
    public Node() {
        this.parent_id = null;
        this.commit_message = "initial commit";
        SimpleDateFormat simpleDate = new SimpleDateFormat("EEE MMM dd hh:mm:ss YYYY");
        Date date = new Date();
        this.timeStamp = simpleDate.format(date) + " -0800";
        this.treeblob = new TreeMap<>();
        stage = stage.preparingStage(this.treeblob);
        this.commit_id = creatingCommitid(this);
    }

    /**
     * initializing all commits except for first one
     */
    public Node(String message, String parentid, String mergeParentID) {
        this.commit_message = message;
        this.parent_id = parentid;
        this.mergeParentID = mergeParentID;
        SimpleDateFormat simpleDate = new SimpleDateFormat("EEE MMM dd hh:mm:ss YYYY");
        Date date = new Date();
        this.timeStamp = simpleDate.format(date) + " -0800";
        //checkContains(stage);
        this.treeblob = new TreeMap<>(stage.trackingBlobs);
        stage = stage.preparingStage(this.treeblob);
        this.commit_id = creatingCommitid(this);
    }


    /**
     * creating the unique sha1 for the commitid
     */
    public void write_to_file(Node n, File dir) {
        byte[] arr = serialize(n);
        File f = join(dir, this.commit_id);
        writeContents(f, arr);
    }

    public void read_from_file(String filename) { //TODO
    }


    public String creatingCommitid(Node node) {
        String commitmsg = node.commit_message;
        String parentid = node.parent_id;
        String time = node.timeStamp;
        String sha1 = Utils.sha1(treeblob.toString() + parentid + commitmsg + time);
        return sha1;
    }

}
/*
    public void addBlob(String name, String id) {
        if (current.muppet.containsKey(this.commit_id)) {
            current.muppet.remove(this.commit_id);
        }
        this.hashblob.put(name, id);
        this.commit_id = creatingCommitid(this);
        current.muppet.put(this.commit_id, this);
    }

    public void replaceBlob(String name, String id) {
        if (current.muppet.containsKey(this.commit_id)) {
            current.muppet.remove(this.commit_id);
        }
        this.hashblob.replace(name, id);
        this.commit_id = creatingCommitid(this);
        current.muppet.put(this.commit_id, this);
    }

    public void checkContains(StagingArea stageArea) {
        for (String name : stageArea.allBlobs.keySet()) {
            if (!(this.hashblob.containsKey(name))) {
                this.addBlob(name, stageArea.allBlobs.get(name));
            } else {
                this.replaceBlob(name, stageArea.allBlobs.get(name));
            }
        }
    }
}
*/