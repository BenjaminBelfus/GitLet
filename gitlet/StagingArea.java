package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

public class StagingArea implements Serializable {
    TreeMap<String, Blob> allBlobs; //rojos = allblobs - trackingblobs
    TreeMap<String, Blob> trackingBlobs; //verde
    TreeMap<String, Blob> removedBlobs; // when remove, you save it, if needed later.

    public StagingArea(TreeMap<String, Blob> selectedFiles) {
        this.allBlobs = new TreeMap<>();
        this.trackingBlobs = new TreeMap<>(selectedFiles);
        this.removedBlobs = new TreeMap<>();
    }

    /** chequear si funca la lesera */
    public StagingArea preparingStage(TreeMap<String, Blob> filesToTrack) { //filestotrack = treemap of name & sha
        StagingArea stageArea = new StagingArea(filesToTrack);
        return stageArea;
    }
}

