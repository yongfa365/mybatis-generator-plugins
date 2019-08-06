package yongfa365.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.util.StringUtility;
import yongfa365.mybatis.generator.Utils.ContextUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TkMapperPlugin extends FalseMethodPlugin {
    private Set<String> mappers = new HashSet<String>();
    private boolean isGenColumnTypeWithJdbcType = false;
    private boolean isMixModelImports = true;

    //shellCallback use TargetProject and TargetPackage to get targetFile
    ShellCallback shellCallback = new DefaultShellCallback(false);


    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        String mappers = properties.getProperty("mappers");
        isGenColumnTypeWithJdbcType = StringUtility.isTrue(properties.getProperty("isGenColumnTypeWithJdbcType"));
        isMixModelImports = StringUtility.isTrue(properties.getProperty("isMixModelImports"));

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
        try {
            String spliter4Model = "    //████████上面是自动生成的，改了会被覆盖，有需要可以添加到下面(不能用@Column,要加上@Transient)，此行不能删除████████";
            Pattern importPattern = Pattern.compile("import\\s+(\\S+)", Pattern.MULTILINE);

            File modelFile = ContextUtils.getModelFile(context, introspectedTable);
            String newFileString = topLevelClass.getFormattedContent();

            if (modelFile.exists()) {
                String oldFileString = ContextUtils.readAllString(modelFile.toPath());
                //imports合并
                if (isMixModelImports) {
                    ArrayList<String> oldImports = new ArrayList<>();
                    Matcher matcher = importPattern.matcher(oldFileString);
                    while (matcher.find()) {
                        oldImports.add(matcher.group(1));
                    }

                    ArrayList<String> newImports = new ArrayList<>();
                    Matcher matcher2 = importPattern.matcher(newFileString);
                    while (matcher2.find()) {
                        newImports.add(matcher2.group(1));
                    }
                    oldImports.removeAll(newImports);
                    for (String item : oldImports) {
                        newFileString = newFileString.replaceAll("package .*", "$0\r\nimport " + item);
                    }
                }

                //将旧文件的分割线后的内容 贴到 新文件最后的“}”之前
                {
                    oldFileString = oldFileString.replaceAll("[\\s\\S]+█████\r\n", "");
                    newFileString = newFileString.substring(0, newFileString.lastIndexOf("}")) + spliter4Model + "\r\n" + oldFileString;
                }
            } else {
                newFileString = newFileString.substring(0, newFileString.lastIndexOf("}")) + spliter4Model + "\r\n}";
            }

            Files.write(modelFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //已经是最终的了，合并后返回false，其他插件就不会再走整个方法了
        return false;
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
            String spliter4Xml = "    <!--████████上面是自动生成的，改了会被覆盖，有需要可以添加到下面，此行不能删除████████-->";

            File directory = shellCallback.getDirectory(sqlMap.getTargetProject(), sqlMap.getTargetPackage());
            File xmlFile = new File(directory, sqlMap.getFileName());

            String newFileString = sqlMap.getFormattedContent();

            if (xmlFile.exists()) {
                String oldFileString = ContextUtils.readAllString(xmlFile.toPath());
                oldFileString = oldFileString.replaceAll("[\\s\\S]+█████-->\r\n", "");
                newFileString = newFileString.replace("</mapper>", spliter4Xml + "\r\n" + oldFileString);
            } else {
                newFileString = newFileString.replace("</mapper>", spliter4Xml + "\r\n</mapper>");
            }

            //MBG合并逻辑与我们预期不同，自行处理下原XML文件
            Files.write(xmlFile.toPath(), newFileString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //已经是最终的了，合并后返回false，其他插件就不会再走整个方法了
        return false;
    }


    //===============Field加@ColumnType(jdbcType = JdbcType.NVARCHAR)=================
    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        if (!isGenColumnTypeWithJdbcType) {
            return true;
        }

        //加上后就跟MyBatis里的写法一样了：#{resourceID,jdbcType=BIGINT}
        if (field.getAnnotations().stream().noneMatch(p -> p.contains("ColumnType"))) {
            topLevelClass.addImportedType("org.apache.ibatis.type.JdbcType");
            topLevelClass.addImportedType("tk.mybatis.mapper.annotation.ColumnType");
            field.addAnnotation("@ColumnType(jdbcType = JdbcType." + introspectedColumn.getJdbcTypeName() + ")");
        }
        return true;
    }
}

