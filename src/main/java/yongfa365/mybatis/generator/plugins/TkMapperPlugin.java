package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.DomWriter;
import org.mybatis.generator.internal.util.StringUtility;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import yongfa365.mybatis.generator.Utils.ContextUtils;
import yongfa365.mybatis.generator.Utils.JavaFileUtils;
import yongfa365.mybatis.generator.Utils.XmlUtils;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//代码摘自：https://github.com/abel533/Mapper/blob/master/generator/src/main/java/tk/mybatis/mapper/generator/FalseMethodPlugin.java
public class TkMapperPlugin extends PluginAdapter {
    private Set<String> mappers = new HashSet<String>();

    //shellCallback use TargetProject and TargetPackage to get targetFile
    ShellCallback shellCallback = new DefaultShellCallback(false);

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        String mappers = properties.getProperty("mappers");
        if (StringUtility.stringHasValue(mappers)) {
            this.mappers.addAll(Arrays.asList(mappers.split(",")));
        } else {
            throw new RuntimeException("Mapper插件缺少必要的mappers属性!");
        }
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    //=============================Model=============================
    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        File modelFile = ContextUtils.getModelFile(context, introspectedTable);
        if (modelFile.exists()) {
            JavaFileUtils.mergerFile(context,topLevelClass,introspectedTable);

            //已经是最终的了，合并后返回false，其他插件就不会再走整个方法了
            return false;
        } else {
            //文件不存在就走正常创建的逻辑
            return true;
        }


    }

    //=============================DAO=============================
    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        File xmlFile = ContextUtils.getDaoFile(context, introspectedTable);
        if (xmlFile.exists()) {
            System.out.println(xmlFile + "已经存在，就不处理了");
            return false;
        }

        interfaze.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
        interfaze.addAnnotation("@Repository");

        //获取实体类
        FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
        //import接口
        for (String mapper : mappers) {
            interfaze.addImportedType(new FullyQualifiedJavaType(mapper));
            interfaze.addSuperInterface(new FullyQualifiedJavaType(mapper + "<" + entityType.getShortName() + ">"));
        }
        //import实体类
        interfaze.addImportedType(entityType);

        return true;
    }

    //=============================XML=============================
    //new nodes is generated,but not write on disk,we just need to filter.
    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        try {
            File directory = shellCallback.getDirectory(sqlMap.getTargetProject(), sqlMap.getTargetPackage());
            File xmlFile = new File(directory, sqlMap.getFileName());
            if (!directory.exists() || !xmlFile.exists()) {
                return true;
            }

            String oldFileString = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);

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

            String spliter = "上面是MBG自动生成的，不要改";
            if (!oldFileString.contains(spliter)) {
                oldFileString = "  <!-- ====================" + spliter + "==================== -->\r\n\r\n" + oldFileString;
            }
            newFileString = newFileString.replace("</mapper>", oldFileString);

            //MBG合并逻辑与我们预期不同，自行处理下原XML文件
            Files.write(xmlFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //不使用MBG的合并逻辑，上面已经自行合并了
        return false;
    }

    //=============================其他一堆直接禁用=============================
    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectAllMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectAllElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerApplyWhereMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }

    @Override
    public boolean providerUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        return false;
    }
}
