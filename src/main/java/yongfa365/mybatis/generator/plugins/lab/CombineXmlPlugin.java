package yongfa365.mybatis.generator.plugins.lab;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.DomWriter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import yongfa365.mybatis.generator.util.ContextUtils;
import yongfa365.mybatis.generator.util.XmlUtils;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            File directory = shellCallback.getDirectory(sqlMap.getTargetProject(), sqlMap.getTargetPackage());
            File xmlFile = new File(directory, sqlMap.getFileName());
            if (!directory.exists() || !xmlFile.exists()) {
                return true;
            }

            String oldFileString = ContextUtils.readAllString(xmlFile.toPath());

            Document newDoc = XmlUtils.getDocumentBuilder().parse(new InputSource(new StringReader(sqlMap.getFormattedContent())));
            String newFileString = new DomWriter().toString(newDoc);

            //新的有的就在旧的里删掉
            Pattern reNewFile = Pattern.compile("<(\\S+)\\s+id=\"(\\S+)\"");
            Matcher matcher = reNewFile.matcher(newFileString);
            while (matcher.find()) {
                String reOldFile = String.format("[\\S\\s]*?<%s\\s+id=\"%s\"[\\S\\s]+?</%1$s>(\r\n)*", matcher.group(1), matcher.group(2));
                oldFileString = oldFileString.replaceAll(reOldFile, "");
            }


            //用新的内容除了最后一行外的，替换旧的里的文件的前半部分。
            oldFileString = oldFileString.replaceAll("[\\S\\s]*?<mapper.+?>(\r\n)*", "");

            String spliter="上面是MBG自动生成的，不要改";
            if (!oldFileString.contains(spliter)) {
                oldFileString = "  <!-- ===================="+spliter+"==================== -->\r\n\r\n" + oldFileString;
            }
            newFileString = newFileString.replace("</mapper>", oldFileString);

            //MBG合并逻辑与我们预期不同，自行处理下原XML文件
            Files.write(xmlFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}