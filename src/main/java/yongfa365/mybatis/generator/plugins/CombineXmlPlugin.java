package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.DomWriter;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User:zhangweixiao
 * Description:
 * old nodes is your existing xml file's first level nodes,like <insert><resultMap>
 * new nodes is mybatis-generator generate for you to combine
 * This compare the first level node's name and "id" attribute of new nodes and old nodes
 * if the two equal,then new node will not generate
 * so this can make you modification in old nodes not override.
 * if you want to regenrate old node,delete it,it will generate new.
 */
public class CombineXmlPlugin extends PluginAdapter {

    //shellCallback use TargetProject and TargetPackage to get targetFile
    ShellCallback shellCallback = new DefaultShellCallback(false);

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }


    //new nodes is generated,but not write on disk,we just need to filter.
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        try {
            //get old nodes
            File directory = shellCallback.getDirectory(sqlMap.getTargetProject(), sqlMap.getTargetPackage());
            File xmlFile = new File(directory, sqlMap.getFileName());
            if (!directory.exists() || !xmlFile.exists()) {
                return true;
            }
            String oldFileString = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);


            Document newDoc = getDocumentBuilder().parse(new InputSource(new StringReader(sqlMap.getFormattedContent())));
            String newFileString = new DomWriter().toString(newDoc);
            Pattern re = Pattern.compile("<(\\S+)\\s+id=\"(\\S+)\"");

            //新的有的就在旧的里删掉
            Matcher matcher = re.matcher(newFileString);
            while (matcher.find()) {
                String re2 = String.format("<{0}\\s+id=\"{1}\"[\\S\\s]+?</{0}>", matcher.group(1), matcher.group(2));
                oldFileString = oldFileString.replaceAll(re2, "");
            }

            //用新的内容除了最后一行外的，替换旧的里的文件的前半部分。
            oldFileString = oldFileString.replaceAll("[\\S\\s]+?<mapper.+?>", "");

            newFileString = newFileString.replace("</mapper>", oldFileString);

            //            NodeList newNodeList = newDoc.getChildNodes();
            //            for (int i = 0; i < newNodeList.getLength(); i++) {
            //                Node node=newNodeList.item(i);
            //
            //                if (node.getNodeType() == Node.ELEMENT_NODE ){
            //                    String re=String.format("<{0}\\s+id=\"{1}\"[\S\S]+?</{0}>",node.getNodeName(),node.getNodeValue());
            //                    oldFileString.replaceAll("<"+, "");
            //                }
            //            }

            //MBG合并逻辑与我们预期不同，自行先处理下原XML文件

            Files.write(xmlFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private Document getDocFromFile(File xmlFile) throws Exception {
        Document existingDocument = getDocumentBuilder().parse(xmlFile);
        return existingDocument;
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new NullEntityResolver());
        return builder;
    }

    private class NullEntityResolver implements EntityResolver {
        /**
         * returns an empty reader. This is done so that the parser doesn't
         * attempt to read a DTD. We don't need that support for the merge and
         * it can cause problems on systems that aren't Internet connected.
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {

            StringReader sr = new StringReader(""); //$NON-NLS-1$

            return new InputSource(sr);
        }
    }
}