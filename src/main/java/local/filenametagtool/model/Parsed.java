package local.filenametagtool.model;

import java.util.List;

public class Parsed {
    public final Action action;
    public final List<String> paths;

    public Parsed(Action action, List<String> paths) {
        this.action = action;
        this.paths = paths;
    }
}