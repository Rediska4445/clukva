package rf.ebanina.utils.io.formats.xml.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class XmlData {
    private String data = "";
    private final Map<String, XmlData> children = new LinkedHashMap<>();

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Map<String, XmlData> getChildren() {
        return children;
    }

    public boolean isBranch() {
        return !children.isEmpty();
    }
}
