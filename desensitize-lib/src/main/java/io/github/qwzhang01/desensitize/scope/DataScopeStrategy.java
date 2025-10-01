package io.github.qwzhang01.desensitize.scope;

import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据权限策略
 *
 * @author avinzhang
 */
public abstract class DataScopeStrategy {

    private static final Logger log = LoggerFactory.getLogger(DataScopeStrategy.class);

    private List<Join> joins;

    public final List<Join> getJoins() {
        if (joins != null) {
            return BeanUtil.copyToList(joins, Join.class);
        }
        configJoin();
        return BeanUtil.copyToList(joins, Join.class);
    }

    /**
     * 获取 Where 条件
     * <p>
     * 1 加缓存，同一个请求 不同拼接多次 where 条件
     * 2 拼接where如果查询数据库，不加数据权限逻辑
     *
     * @return
     */
    public final List<Where> getWheres() {
        DataScopeHelper.Context context = DataScopeHelper.get();
        try {
            if (context != null) {
                List<Where> wheres = context.getWheres();
                if (wheres != null && !wheres.isEmpty()) {
                    return SerializationUtils.clone(new ArrayList<>(wheres));
                }
            }

            DataScopeHelper.clear();
            List<Where> wheres = configWhere();
            if (wheres != null && !wheres.isEmpty() && context != null) {
                context.setWheres(wheres);
                return SerializationUtils.clone(new ArrayList<>(wheres));
            }
            return Collections.emptyList();
        } finally {
            DataScopeHelper.set(context);
        }
    }

    public final List<Where> addWhere(String table, String column, String oneOp, String oneValue) {
        List<Where> list = new ArrayList<>();
        list.add(OneWhere.builderOneWhere().oneOp(oneOp).oneValue(oneValue).table(table).column(column).build());
        return list;
    }

    public final <T> List<Where> addWhere(String column, String multiOp, List<T> multiValue) {
        return addWhere("", column, multiOp, multiValue);
    }

    public final <T> List<Where> addWhere(String column, Number from, Number to) {
        List<Where> list = new ArrayList<>();
        list.add(BetweenWhere.builderBtWhere().column(column)
                .multiOp(SqlKeyword.BETWEEN.getSqlSegment())
                .from(from).to(to).build());
        return list;
    }

    public final <T> List<Where> addWhere(String table, String column, String multiOp, List<T> multiValue) {
        List<Where> list = new ArrayList<>();
        InWhere<T> inWhere = new InWhere<>();
        inWhere.setTable(table);
        inWhere.setColumn(column);
        inWhere.setMultiOp(multiOp);
        inWhere.setMultiValue(multiValue);
        list.add(inWhere);
        return list;
    }

    public final List<Where> addWhere(String sql) {
        SqlWhere where = SqlWhere.builderSqlWhere().sql(sql).build();
        List<Where> list = new ArrayList<>();
        list.add(where);
        return list;
    }

    public final void addJoin(String dataScopeTable, String mainTableColumn, String dataScopeTableColumn) {
        addJoin(null, dataScopeTable, mainTableColumn, dataScopeTableColumn);
    }

    /**
     * join 没有动态变量，只初始化一次
     *
     * @param mainTable
     * @param dataScopeTable
     * @param mainTableColumn
     * @param dataScopeTableColumn
     */
    public final synchronized void addJoin(String mainTable, String dataScopeTable, String mainTableColumn, String dataScopeTableColumn) {
        if (joins == null) {
            this.joins = new ArrayList<>();
        }

        Join join = new Join();
        joins.add(join.setDataScopeTable(dataScopeTable).setOnColumns(Collections.singletonList(new OnColumn().setMainTableColumn(mainTableColumn).setDataScopeTableColumn(dataScopeTableColumn))));
    }

    /**
     * 数据权限设置 join 表信息
     */
    protected abstract void configJoin();

    /**
     * 数据权限设置 Where 条件
     */
    protected abstract List<Where> configWhere();

    public static final class Join {
        private String mainTable;
        private String dataScopeTable;
        private List<OnColumn> onColumns;
        private List<OnConst> onMainTableConsts;
        private List<OnConst> onDataScopeConsts;

        /**
         * 空构造函数
         */
        public Join() {
        }

        /**
         * 全参数构造函数
         */
        public Join(String mainTable, String dataScopeTable, List<OnColumn> onColumns,
                    List<OnConst> onMainTableConsts, List<OnConst> onDataScopeConsts) {
            this.mainTable = mainTable;
            this.dataScopeTable = dataScopeTable;
            this.onColumns = onColumns;
            this.onMainTableConsts = onMainTableConsts;
            this.onDataScopeConsts = onDataScopeConsts;
        }

        public static JoinBuilder builder() {
            return new JoinBuilder();
        }

        public String getMainTable() {
            return mainTable;
        }

        public Join setMainTable(String mainTable) {
            this.mainTable = mainTable;
            return this;
        }

        public String getDataScopeTable() {
            return dataScopeTable;
        }

        public Join setDataScopeTable(String dataScopeTable) {
            this.dataScopeTable = dataScopeTable;
            return this;
        }

        public List<OnColumn> getOnColumns() {
            return onColumns;
        }

        public Join setOnColumns(List<OnColumn> onColumns) {
            this.onColumns = onColumns;
            return this;
        }

        public List<OnConst> getOnMainTableConsts() {
            return onMainTableConsts;
        }

        public Join setOnMainTableConsts(List<OnConst> onMainTableConsts) {
            this.onMainTableConsts = onMainTableConsts;
            return this;
        }

        public List<OnConst> getOnDataScopeConsts() {
            return onDataScopeConsts;
        }

        public Join setOnDataScopeConsts(List<OnConst> onDataScopeConsts) {
            this.onDataScopeConsts = onDataScopeConsts;
            return this;
        }

        public static class JoinBuilder {
            private String mainTable;
            private String dataScopeTable;
            private List<OnColumn> onColumns;
            private List<OnConst> onMainTableConsts;
            private List<OnConst> onDataScopeConsts;

            public JoinBuilder mainTable(String mainTable) {
                this.mainTable = mainTable;
                return this;
            }

            public JoinBuilder dataScopeTable(String dataScopeTable) {
                this.dataScopeTable = dataScopeTable;
                return this;
            }

            public JoinBuilder onColumns(List<OnColumn> onColumns) {
                this.onColumns = onColumns;
                return this;
            }

            public JoinBuilder onMainTableConsts(List<OnConst> onMainTableConsts) {
                this.onMainTableConsts = onMainTableConsts;
                return this;
            }

            public JoinBuilder onDataScopeConsts(List<OnConst> onDataScopeConsts) {
                this.onDataScopeConsts = onDataScopeConsts;
                return this;
            }

            public Join build() {
                return new Join(mainTable, dataScopeTable, onColumns, onMainTableConsts, onDataScopeConsts);
            }
        }
    }

    public static final class OnColumn {
        private String mainTableColumn;
        private String dataScopeTableColumn;

        /**
         * 空构造函数
         */
        public OnColumn() {
        }

        /**
         * 全参数构造函数
         */
        public OnColumn(String mainTableColumn, String dataScopeTableColumn) {
            this.mainTableColumn = mainTableColumn;
            this.dataScopeTableColumn = dataScopeTableColumn;
        }

        public static OnColumnBuilder builder() {
            return new OnColumnBuilder();
        }

        public String getMainTableColumn() {
            return mainTableColumn;
        }

        public OnColumn setMainTableColumn(String mainTableColumn) {
            this.mainTableColumn = mainTableColumn;
            return this;
        }

        public String getDataScopeTableColumn() {
            return dataScopeTableColumn;
        }

        public OnColumn setDataScopeTableColumn(String dataScopeTableColumn) {
            this.dataScopeTableColumn = dataScopeTableColumn;
            return this;
        }

        public static class OnColumnBuilder {
            private String mainTableColumn;
            private String dataScopeTableColumn;

            public OnColumnBuilder mainTableColumn(String mainTableColumn) {
                this.mainTableColumn = mainTableColumn;
                return this;
            }

            public OnColumnBuilder dataScopeTableColumn(String dataScopeTableColumn) {
                this.dataScopeTableColumn = dataScopeTableColumn;
                return this;
            }

            public OnColumn build() {
                return new OnColumn(mainTableColumn, dataScopeTableColumn);
            }
        }
    }

    public static final class OnConst {
        private String column;
        private String consts;

        /**
         * 空构造函数
         */
        public OnConst() {
        }

        /**
         * 全参数构造函数
         */
        public OnConst(String column, String consts) {
            this.column = column;
            this.consts = consts;
        }

        public static OnConstBuilder builder() {
            return new OnConstBuilder();
        }

        public String getColumn() {
            return column;
        }

        public OnConst setColumn(String column) {
            this.column = column;
            return this;
        }

        public String getConsts() {
            return consts;
        }

        public OnConst setConsts(String consts) {
            this.consts = consts;
            return this;
        }

        public static class OnConstBuilder {
            private String column;
            private String consts;

            public OnConstBuilder column(String column) {
                this.column = column;
                return this;
            }

            public OnConstBuilder consts(String consts) {
                this.consts = consts;
                return this;
            }

            public OnConst build() {
                return new OnConst(column, consts);
            }
        }
    }

    public static class Where implements Serializable {
        private String table;
        private String column;

        /**
         * 空构造函数
         */
        public Where() {
        }

        /**
         * 全参数构造函数
         */
        public Where(String table, String column) {
            this.table = table;
            this.column = column;
        }

        public static WhereBuilder builder() {
            return new WhereBuilder();
        }

        public String getTable() {
            return table;
        }

        public Where setTable(String table) {
            this.table = table;
            return this;
        }

        public String getColumn() {
            return column;
        }

        public Where setColumn(String column) {
            this.column = column;
            return this;
        }

        public static class WhereBuilder {
            private String table;
            private String column;

            public WhereBuilder table(String table) {
                this.table = table;
                return this;
            }

            public WhereBuilder column(String column) {
                this.column = column;
                return this;
            }

            public Where build() {
                return new Where(table, column);
            }
        }
    }

    public static class SqlWhere extends Where {
        private String sql;

        /**
         * 空构造函数
         */
        public SqlWhere() {
            super();
        }

        /**
         * 全参数构造函数
         */
        public SqlWhere(String table, String column, String sql) {
            super(table, column);
            this.sql = sql;
        }

        /**
         * 构造函数（仅 SQL）
         */
        public SqlWhere(String sql) {
            this.sql = sql;
        }

        public static SqlWhereBuilder builderSqlWhere() {
            return new SqlWhereBuilder();
        }

        public String getSql() {
            return sql;
        }

        public SqlWhere setSql(String sql) {
            this.sql = sql;
            return this;
        }

        @Override
        public SqlWhere setTable(String table) {
            super.setTable(table);
            return this;
        }

        @Override
        public SqlWhere setColumn(String column) {
            super.setColumn(column);
            return this;
        }

        public static class SqlWhereBuilder {
            private String table;
            private String column;
            private String sql;

            public SqlWhereBuilder table(String table) {
                this.table = table;
                return this;
            }

            public SqlWhereBuilder column(String column) {
                this.column = column;
                return this;
            }

            public SqlWhereBuilder sql(String sql) {
                this.sql = sql;
                return this;
            }

            public SqlWhere build() {
                return new SqlWhere(table, column, sql);
            }
        }
    }

    public static final class OneWhere extends Where {
        /**
         * 运算符号
         * = > < >= <= like exits isnull notnull
         *
         * @see SqlKeyword
         */
        private String oneOp;
        /**
         * 查询值 单个
         */
        private String oneValue;

        /**
         * 空构造函数
         */
        public OneWhere() {
            super();
        }

        /**
         * 全参数构造函数
         */
        public OneWhere(String table, String column, String oneOp, String oneValue) {
            super(table, column);
            this.oneOp = oneOp;
            this.oneValue = oneValue;
        }

        public static OneWhereBuilder builderOneWhere() {
            return new OneWhereBuilder();
        }

        public String getOneOp() {
            return oneOp;
        }

        public OneWhere setOneOp(String oneOp) {
            this.oneOp = oneOp;
            return this;
        }

        public String getOneValue() {
            return oneValue;
        }

        public OneWhere setOneValue(String oneValue) {
            this.oneValue = oneValue;
            return this;
        }

        @Override
        public OneWhere setTable(String table) {
            super.setTable(table);
            return this;
        }

        @Override
        public OneWhere setColumn(String column) {
            super.setColumn(column);
            return this;
        }

        public static class OneWhereBuilder {
            private String table;
            private String column;
            private String oneOp;
            private String oneValue;

            public OneWhereBuilder table(String table) {
                this.table = table;
                return this;
            }

            public OneWhereBuilder column(String column) {
                this.column = column;
                return this;
            }

            public OneWhereBuilder oneOp(String oneOp) {
                this.oneOp = oneOp;
                return this;
            }

            public OneWhereBuilder oneValue(String oneValue) {
                this.oneValue = oneValue;
                return this;
            }

            public OneWhere build() {
                return new OneWhere(table, column, oneOp, oneValue);
            }
        }
    }

    public static final class InWhere<T> extends Where {
        /**
         * 多个值运算符
         * in notin
         *
         * @see SqlKeyword
         */
        private String multiOp;
        /**
         * 查询值 列表
         */
        private List<T> multiValue;

        /**
         * 空构造函数
         */
        public InWhere() {
            super();
        }

        /**
         * 全参数构造函数
         */
        public InWhere(String table, String column, String multiOp, List<T> multiValue) {
            super(table, column);
            this.multiOp = multiOp;
            this.multiValue = multiValue;
        }

        public static <T> InWhereBuilder<T> builderInWhere() {
            return new InWhereBuilder<>();
        }

        public String getMultiOp() {
            return multiOp;
        }

        public InWhere<T> setMultiOp(String multiOp) {
            this.multiOp = multiOp;
            return this;
        }

        public List<T> getMultiValue() {
            return multiValue;
        }

        public InWhere<T> setMultiValue(List<T> multiValue) {
            this.multiValue = multiValue;
            return this;
        }

        @Override
        public InWhere<T> setTable(String table) {
            super.setTable(table);
            return this;
        }

        @Override
        public InWhere<T> setColumn(String column) {
            super.setColumn(column);
            return this;
        }

        public static class InWhereBuilder<T> {
            private String table;
            private String column;
            private String multiOp;
            private List<T> multiValue;

            public InWhereBuilder<T> table(String table) {
                this.table = table;
                return this;
            }

            public InWhereBuilder<T> column(String column) {
                this.column = column;
                return this;
            }

            public InWhereBuilder<T> multiOp(String multiOp) {
                this.multiOp = multiOp;
                return this;
            }

            public InWhereBuilder<T> multiValue(List<T> multiValue) {
                this.multiValue = multiValue;
                return this;
            }

            public InWhere<T> build() {
                return new InWhere<>(table, column, multiOp, multiValue);
            }
        }
    }

    public static final class BetweenWhere extends Where {
        /**
         * 多个值运算符
         * between
         *
         * @see SqlKeyword
         */
        private String multiOp = SqlKeyword.BETWEEN.getSqlSegment();
        /**
         * between 的 from to
         */
        private Number from;
        private Number to;

        /**
         * 空构造函数
         */
        public BetweenWhere() {
            super();
        }

        /**
         * 全参数构造函数
         */
        public BetweenWhere(String table, String column, String multiOp, Number from, Number to) {
            super(table, column);
            this.multiOp = multiOp;
            this.from = from;
            this.to = to;
        }

        /**
         * 简化构造函数
         */
        public BetweenWhere(String column, Number from, Number to) {
            super(null, column);
            this.from = from;
            this.to = to;
        }

        public static BetweenWhereBuilder builderBtWhere() {
            return new BetweenWhereBuilder();
        }

        public String getMultiOp() {
            return multiOp;
        }

        public BetweenWhere setMultiOp(String multiOp) {
            this.multiOp = multiOp;
            return this;
        }

        public Number getFrom() {
            return from;
        }

        public BetweenWhere setFrom(Number from) {
            this.from = from;
            return this;
        }

        public Number getTo() {
            return to;
        }

        public BetweenWhere setTo(Number to) {
            this.to = to;
            return this;
        }

        @Override
        public BetweenWhere setTable(String table) {
            super.setTable(table);
            return this;
        }

        @Override
        public BetweenWhere setColumn(String column) {
            super.setColumn(column);
            return this;
        }

        public static class BetweenWhereBuilder {
            private String table;
            private String column;
            private String multiOp = SqlKeyword.BETWEEN.getSqlSegment();
            private Number from;
            private Number to;

            public BetweenWhereBuilder table(String table) {
                this.table = table;
                return this;
            }

            public BetweenWhereBuilder column(String column) {
                this.column = column;
                return this;
            }

            public BetweenWhereBuilder multiOp(String multiOp) {
                this.multiOp = multiOp;
                return this;
            }

            public BetweenWhereBuilder from(Number from) {
                this.from = from;
                return this;
            }

            public BetweenWhereBuilder to(Number to) {
                this.to = to;
                return this;
            }

            public BetweenWhere build() {
                return new BetweenWhere(table, column, multiOp, from, to);
            }
        }
    }

    public static final class ExistWhere extends Where {
        private String existTable;
        private List<Where> wheres;

        /**
         * 空构造函数
         */
        public ExistWhere() {
            super();
        }

        /**
         * 全参数构造函数
         */
        public ExistWhere(String table, String column, String existTable, List<Where> wheres) {
            super(table, column);
            this.existTable = existTable;
            this.wheres = wheres;
        }

        /**
         * 简化构造函数
         */
        public ExistWhere(String existTable, List<Where> wheres) {
            this.existTable = existTable;
            this.wheres = wheres;
        }

        public static ExistWhereBuilder builderExistWhere() {
            return new ExistWhereBuilder();
        }

        public String getExistTable() {
            return existTable;
        }

        public ExistWhere setExistTable(String existTable) {
            this.existTable = existTable;
            return this;
        }

        public List<Where> getWheres() {
            return wheres;
        }

        public ExistWhere setWheres(List<Where> wheres) {
            this.wheres = wheres;
            return this;
        }

        @Override
        public ExistWhere setTable(String table) {
            super.setTable(table);
            return this;
        }

        @Override
        public ExistWhere setColumn(String column) {
            super.setColumn(column);
            return this;
        }

        public static class ExistWhereBuilder {
            private String table;
            private String column;
            private String existTable;
            private List<Where> wheres;

            public ExistWhereBuilder table(String table) {
                this.table = table;
                return this;
            }

            public ExistWhereBuilder column(String column) {
                this.column = column;
                return this;
            }

            public ExistWhereBuilder existTable(String existTable) {
                this.existTable = existTable;
                return this;
            }

            public ExistWhereBuilder wheres(List<Where> wheres) {
                this.wheres = wheres;
                return this;
            }

            public ExistWhere build() {
                return new ExistWhere(table, column, existTable, wheres);
            }
        }
    }
}