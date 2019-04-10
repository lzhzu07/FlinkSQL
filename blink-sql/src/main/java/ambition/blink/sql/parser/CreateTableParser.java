package ambition.blink.sql.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ambition.blink.common.utils.BlinkStringUtils;

/**
 * @Author: wpl
 */
public class CreateTableParser {

    public static CreateTableParser newInstance(){
        return new CreateTableParser();
    }

    public SqlParserResult parseSql(String sql, Pattern pattern) {
        SqlParserResult result = null;
        sql = BlinkStringUtils.getReplaceString(sql);
        Matcher matcher = pattern.matcher(sql);
        if(matcher.find()){
            String tableName = matcher.group(1);
            String fieldsInfoStr = matcher.group(2);
            String propsStr = matcher.group(3);
            Map<String, String> props = parseProp(propsStr);

            result = new SqlParserResult();
            result.setTableName(tableName);
            result.setFieldsInfoStr(fieldsInfoStr);
            result.setPropMap(props);

        }
        return result;
    }

    private Map parseProp(String propsStr){
        String[] strs = BlinkStringUtils.splitIgnoreQuotaBrackets(propsStr.trim(),",");
        Map<String, String> propMap = new LinkedHashMap();
        for(int i=0; i<strs.length; i++){
            List<String> ss = BlinkStringUtils.splitIgnoreQuota(strs[i], '=');
            String key = ss.get(0).trim().replaceAll("'", "").trim();
            String value = ss.get(1).trim().replaceAll("'", "").trim();
            propMap.put(key, value);
        }

        return propMap;
    }

    public class SqlParserResult{

        private String tableName;

        private String fieldsInfoStr;

        private Map<String, String> propMap;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getFieldsInfoStr() {
            return fieldsInfoStr;
        }

        public void setFieldsInfoStr(String fieldsInfoStr) {
            this.fieldsInfoStr = fieldsInfoStr;
        }

        public Map<String, String> getPropMap() {
            return propMap;
        }

        public void setPropMap(Map<String, String> propMap) {
            this.propMap = propMap;
        }
    }
}

