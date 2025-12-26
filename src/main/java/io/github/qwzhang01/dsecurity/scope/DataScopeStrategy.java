package io.github.qwzhang01.dsecurity.scope;

import java.util.List;

/**
 * Data scope strategy interface.
 *
 * <p>Defines core methods for data permission filtering by adding permission
 * control conditions to SQL queries.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * public class DepartmentScopeStrategy implements DataScopeStrategy&lt;Long&gt; {
 *     {@code @Override}
 *     public String join() {
 *         return "LEFT JOIN department d ON t.dept_id = d.id";
 *     }
 *     
 *     {@code @Override}
 *     public String where() {
 *         return "d.id IN (SELECT dept_id FROM user_dept WHERE user_id = ?)";
 *     }
 * }
 * </pre>
 *
 * @param <T> permission data type, typically permission ID or permission object
 * @author avinzhang
 */
public interface DataScopeStrategy<T> {
    /**
     * Data scope JOIN table information.
     *
     * <p><strong>Important:</strong> For associated tables not in the original
     * SQL, use full table names without aliases.</p>
     *
     * @return JOIN clause string, or empty string if no join is needed
     */
    String join();

    /**
     * Data scope WHERE condition.
     *
     * <p><strong>Important:</strong> For subqueries or EXISTS conditions with
     * tables not in the original SQL, use full table names.</p>
     *
     * @return WHERE condition string, or empty string if no additional
     * condition is needed
     */
    String where();

    /**
     * Validates and sets effective permission data.
     *
     * <p>Called before SQL execution to process and validate permission data.</p>
     *
     * @param validRights list of valid permission data
     */
    void validDs(List<T> validRights);

    /**
     * Validates and sets effective permission data with whitelist support.
     *
     * <p>The relationship between validRights and withoutRights is OR - if
     * either validation passes, the check succeeds.</p>
     *
     * @param validRights   list of valid permission data
     * @param withoutRights whitelist that bypasses permission validation
     */
    void validDs(List<T> validRights, List<T> withoutRights);
}