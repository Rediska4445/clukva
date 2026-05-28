package rf.ebanina;

import rf.ebanina.utils.io.formats.xml.Xml;
import rf.ebanina.utils.io.formats.xml.dto.XmlData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<cache-struct>\n" +
                "    <directory name=\"cache\">\n" +
                "        <directory name=\"cache\">\n" +
                "            <directory name=\"playlists\"/>\n" +
                "            <directory name=\"tags\"/>\n" +
                "            <directory name=\"tracks\"/>\n" +
                "            <file name=\"history.txt\" content=\"\"/>\n" +
                "        </directory>\n" +
                "        <directory name=\"inet\"/>\n" +
                "        <file name=\"shared.txt\">\n" +
                "            <![CDATA[\n" +
                "            array of (app) = [license_agreed=false, full_time=0];\n" +
                "            array of (main_window) = [right_list_open=true];\n" +
                "            ]]>\n" +
                "        </file>\n" +
                "    </directory>\n" +
                "</cache-struct>";

        try (InputStream stream = new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8))) {
            Map<String, XmlData> result = Xml.parseXmlToXmlData(stream);

            printTree(result, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printTree(Map<String, XmlData> nodes, int depth) {
        String indent = "  ".repeat(depth);

        for (Map.Entry<String, XmlData> entry : nodes.entrySet()) {
            String name = entry.getKey();
            XmlData node = entry.getValue();

            if (node.isBranch()) {
                System.out.println(indent + "📂 Папка/Ветка: [" + name + "]");
                // Рекурсивный уход вглубь до бесконечности
                printTree(node.getChildren(), depth + 1);
            } else {
                System.out.println(indent + "📄 Файл/Лист: [" + name + "]");
                if (!node.getData().isEmpty()) {
                    System.out.println(indent + "   Данные: " + node.getData().replace("\n", "\n" + indent + "   "));
                }
            }
        }
    }

    public static void createDiskStructure(Path currentPath, Map<String, XmlData> nodes) throws IOException {
        for (Map.Entry<String, XmlData> entry : nodes.entrySet()) {
            String name = entry.getKey();
            XmlData nodeData = entry.getValue();
            Path target = currentPath.resolve(name);

            if (nodeData.isBranch()) {
                Files.createDirectories(target);
                createDiskStructure(target, nodeData.getChildren());
            } else {
                Files.createDirectories(target.getParent());
                Files.writeString(target, nodeData.getData(), StandardCharsets.UTF_8);
            }
        }
    }
}
