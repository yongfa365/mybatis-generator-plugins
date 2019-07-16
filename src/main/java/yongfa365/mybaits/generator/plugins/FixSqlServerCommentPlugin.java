package yongfa365.mybaits.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import yongfa365.mybaits.generator.Utils.RemarkUtil;

import java.util.List;

public class FixSqlServerCommentPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        RemarkUtil.generateTableColumnRemark(context);
        return true;
    }


    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        introspectedTable.setRemarks(RemarkUtil.getRemark(introspectedTable, null));
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        introspectedColumn.setRemarks(RemarkUtil.getRemark(introspectedTable, introspectedColumn));
        return true;
    }

}
