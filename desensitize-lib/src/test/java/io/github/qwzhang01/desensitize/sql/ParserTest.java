package io.github.qwzhang01.desensitize.sql;

import io.github.qwzhang01.sql.tool.wrapper.SqlParser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void test() throws JSQLParserException {
        String sql = "SELECT  id,questionId,`input`,`output`,createTime,createBy,updateTime,updateBy,enableFlag  FROM `use_case`  WHERE `enableFlag`=true     AND (`questionId` IN (?,?,?,?,?,?))";
        Statement statement = CCJSqlParserUtil.parse(sql);
        System.out.println(statement);
    }
}
