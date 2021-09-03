package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    byte[] contents;
    String sha;

    public Blob(byte[] contents) {
        this.contents = contents;
        this.sha = Utils.sha1(contents);
    }
}
