package io.github.qwzhang01.desensitize.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserTest {
    @Test
    public void test() throws JSQLParserException {
        String sql = "SELECT  id,questionId,`input`,`output`,createTime,createBy,updateTime,updateBy,enableFlag  FROM `use_case`  WHERE `enableFlag`=true     AND (`questionId` IN (?,?,?,?,?,?))";
        Statement statement = CCJSqlParserUtil.parse(sql);
        System.out.println(statement);
    }

    private static final Pattern p = Pattern.compile("#\\{([^}]*)\\}");

    @Test
    public void test1() throws JSQLParserException {
        String sql = """
                select * from a where 
                (
                    `examVenueId` = #{ew.paramNameValuePairs.MPGENVAL1} 
                    AND (`phone` = #{ew.paramNameValuePairs.MPGENVAL2} OR `email` = #{ew.paramNameValuePairs.MPGENVAL3}) 
                    AND `createTime` >= #{ew.paramNameValuePairs.MPGENVAL4} 
                    AND `createTime` <= #{ew.paramNameValuePairs.MPGENVAL5}
                )
                """;
        Matcher m = p.matcher(sql);
        sql = m.replaceAll("$1");
        System.out.println(sql);
        sql = sql.replaceAll("ew.paramNameValuePairs.", "");
        System.out.println(sql);

        Statement statement = CCJSqlParserUtil.parse(sql);



        System.out.println(statement);
    }
}
