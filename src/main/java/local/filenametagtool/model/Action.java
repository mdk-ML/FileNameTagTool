package local.filenametagtool.model;

public enum Action {
    ADD("add"), 
    REMOVE_ALL("removeAll"), 
    REMOVE("remove"), 
    NEW_VERSION("newVersion"), 
    COPY_WITHOUT_TAGS("copyWithoutTags"), 
    SEARCH("search");

    public final String arg;

    Action(String arg) {
        this.arg = arg;
    }

    public static Action fromArg(String s) {
        if (s == null) return null;
        for (Action a : values()) {
            if (a.arg.equalsIgnoreCase(s.trim())) return a;
        }
        return null;
    }
}