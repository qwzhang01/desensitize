# Seven Data Security

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.qwzhang01/seven-data-security.svg)](https://search.maven.org/artifact/io.github.qwzhang01/seven-data-security)
[![Java Version](https://img.shields.io/badge/Java-17%2B-green.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

[English Documentation](README.md)

**Seven Data Security** 是一个功能全面的 Spring Boot 库,专为 MyBatis 提供透明的数据加密、脱敏和细粒度的数据权限控制。它无缝集成 MyBatis/MyBatis-Plus,自动处理敏感数据保护,无需修改业务逻辑代码。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
  - [字段加密](#字段加密)
  - [查询参数加密](#查询参数加密)
  - [数据权限控制](#数据权限控制)
  - [SQL打印](#sql打印)
- [配置说明](#配置说明)
- [高级用法](#高级用法)
- [设计模式](#设计模式)
- [性能优化](#性能优化)
- [贡献指南](#贡献指南)
- [开源协议](#开源协议)

## ✨ 功能特性

### 🔐 自动数据加密
- **注解驱动的透明加解密**
- **支持多种加密算法**(DES、AES、自定义算法)
- **零业务逻辑侵入** - 拦截器自动处理加解密
- **类型安全**的 Encrypt 包装类型

### 🔍 查询参数加密
- **自动参数检测**和加密
- **支持多种参数类型**: Map、Object、MyBatis-Plus QueryWrapper
- **智能字段匹配** - 支持驼峰和下划线命名转换
- **线程安全的参数还原**机制

### 🎯 数据权限控制
- **基于用户权限的行级数据过滤**
- **灵活的策略模式** - 自定义访问规则
- **SQL重写** - 自动添加 JOIN 和 WHERE 条件
- **多租户支持** - 适用于 SaaS 应用

### 📊 开发工具
- **SQL语句打印** - 显示实际参数值
- **执行时间追踪** - 性能监控
- **环境感知** - 生产环境自动禁用

### 🏗️ 企业级设计
- **线程安全** - ThreadLocal 上下文管理
- **懒加载与缓存** - 优化性能
- **Spring Boot 自动配置**
- **可扩展架构** - 基于接口和抽象类

## 🏛️ 架构设计

```
seven-data-security
├── config                  # 自动配置类
│   ├── JacksonConfig              # JSON序列化配置
│   ├── MaskAutoConfig             # 主配置类
│   └── MyBatisInterceptorAutoConfig  # MyBatis拦截器配置
├── domain                  # 核心领域模型
│   ├── AnnotatedField             # 字段注解元数据
│   ├── Encrypt                    # 加密包装类型
│   ├── ParameterEncryptInfo       # 参数加密上下文
│   └── ParameterRestoreInfo       # 参数还原上下文
├── encrypt                 # 加密子系统
│   ├── annotation                 # @EncryptField 注解
│   ├── container                  # 算法和元数据容器
│   ├── context                    # 加密上下文管理
│   ├── jackson                    # JSON序列化支持
│   ├── processor                  # 加解密处理器
│   ├── shield                     # 加密算法实现
│   └── type/handler               # MyBatis 类型处理器
├── interceptor             # MyBatis 拦截器
│   ├── DecryptInterceptor         # 结果解密
│   ├── SqlPrintInterceptor        # SQL日志
│   └── SqlRewriteInterceptor      # 参数加密和数据权限
├── kit                     # 工具类
│   ├── ClazzUtil                  # 反射工具
│   ├── FieldMatchUtil             # 字段匹配逻辑
│   ├── ParamUtil                  # 参数处理
│   ├── SpringContextUtil          # Spring上下文访问
│   ├── SqlPrint                   # SQL格式化
│   └── StringUtil                 # 字符串操作
├── scope                   # 数据权限子系统
│   ├── DataScopeHelper            # 权限上下文管理
│   ├── DataScopeStrategy          # 策略接口
│   ├── container                  # 策略容器
│   └── processor                  # SQL重写处理器
└── exception               # 异常体系
    ├── DesensitizeException       # 基础异常
    └── JacksonException           # JSON相关异常
```

## 🚀 快速开始

### 1. 添加依赖

**Maven:**
```xml
<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>seven-data-security</artifactId>
    <version>1.2.17</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.qwzhang01:seven-data-security:1.2.17'
```

### 2. 定义加密实体

```java
@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    
    private String username;
    
    // 自动加密/解密
    @EncryptField
    private String phoneNumber;
    
    @EncryptField
    private String email;
    
    // 自定义加密算法
    @EncryptField(CustomAesAlgo.class)
    private String socialSecurityNumber;
}
```

### 3. 在服务中使用

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    // 插入前自动加密
    public void createUser(User user) {
        userMapper.insert(user);
    }
    
    // 查询后自动解密
    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
    
    // 查询参数自动加密
    public List<User> findByPhone(String phone) {
        return userMapper.selectList(
            new QueryWrapper<User>().eq("phone_number", phone)
        );
    }
}
```

就这么简单!无需额外代码,库会自动处理加解密。

## 🔑 核心功能

### 字段加密

#### 使用 @EncryptField 注解

```java
@Data
@TableName("customer")
public class Customer {
    @TableId
    private Long id;
    
    // 使用默认加密算法
    @EncryptField
    private String phoneNumber;
    
    // 指定自定义加密算法
    @EncryptField(AesEncryptionAlgo.class)
    private String creditCard;
    
    private String name;
}
```

#### 使用 Encrypt 类型包装

```java
@Data
@TableName("sensitive_data")
public class SensitiveData {
    @TableId
    private Long id;
    
    // 类型安全的加密包装
    private Encrypt secretData;
    
    // Getter 返回明文
    public String getSecretData() {
        return secretData.getValue();
    }
    
    // Setter 接受明文
    public void setSecretData(String value) {
        this.secretData = new Encrypt(value);
    }
}
```

#### 自定义加密算法

```java
public class AesEncryptionAlgo implements EncryptionAlgo {
    private static final String KEY = "YourSecretKey123"; // 生产环境使用环境变量
    
    @Override
    public String encrypt(String value) {
        if (value == null) return null;
        // 实现 AES 加密
        return AesUtils.encrypt(value, KEY);
    }
    
    @Override
    public String decrypt(String value) {
        if (value == null) return null;
        // 实现 AES 解密
        return AesUtils.decrypt(value, KEY);
    }
}
```

#### 注册自定义算法

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public AesEncryptionAlgo aesEncryptionAlgo() {
        return new AesEncryptionAlgo();
    }
}
```

### 查询参数加密

库会自动加密匹配加密字段的查询参数:

```java
// 这些查询都会自动加密 phone 参数
public class UserService {
    
    // Map 参数
    public List<User> findByPhone(String phone) {
        Map<String, Object> params = new HashMap<>();
        params.put("phoneNumber", phone);
        return userMapper.selectByMap(params);
    }
    
    // 对象参数
    public List<User> findUsers(UserQuery query) {
        return userMapper.selectList(query); // query.phoneNumber 自动加密
    }
    
    // QueryWrapper 参数 (MyBatis-Plus)
    public List<User> findWithWrapper(String phone) {
        return userMapper.selectList(
            new QueryWrapper<User>().eq("phone_number", phone) // 自动加密
        );
    }
    
    // XML Mapper 参数
    public List<User> findInXml(String phone) {
        return userMapper.findByPhone(phone); // 自动加密
    }
}
```

**XML Mapper 示例:**
```xml
<select id="findByPhone" resultType="User">
    SELECT * FROM user WHERE phone_number = #{phoneNumber}
    <!-- phoneNumber 在执行前自动加密 -->
</select>
```

### 数据权限控制

基于用户权限实现细粒度的数据访问控制:

#### 1. 定义数据权限策略

```java
@Component
public class DepartmentDataScopeStrategy implements DataScopeStrategy<Long> {
    
    @Autowired
    private UserContext userContext;
    
    @Override
    public String join() {
        // 如需要,添加 JOIN 子句
        return "LEFT JOIN department d ON t.dept_id = d.id";
    }
    
    @Override
    public String where() {
        // 添加权限过滤的 WHERE 条件
        List<Long> deptIds = userContext.getUserDepartmentIds();
        return "d.id IN (" + StringUtils.join(deptIds, ",") + ")";
    }
    
    @Override
    public void validDs(List<Long> validRights) {
        // INSERT/UPDATE/DELETE 时的权限验证
        Long currentDeptId = userContext.getCurrentDepartmentId();
        if (!validRights.contains(currentDeptId)) {
            throw new PermissionDeniedException("无此部门的操作权限");
        }
    }
    
    @Override
    public void validDs(List<Long> validRights, List<Long> withoutRights) {
        // 白名单支持: 如果在白名单中,跳过验证
        Long currentDeptId = userContext.getCurrentDepartmentId();
        if (withoutRights.contains(currentDeptId)) {
            return;
        }
        validDs(validRights);
    }
}
```

#### 2. 在查询中应用数据权限

```java
@Service
public class EmployeeService {
    
    @Autowired
    private EmployeeMapper employeeMapper;
    
    // 应用部门级数据权限
    public List<Employee> getEmployees() {
        return DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setSearchRight(getCurrentUserDeptIds())
            .execute(() -> employeeMapper.selectAll());
    }
    
    // 更新时带权限验证
    public void updateEmployee(Employee employee) {
        DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setValidRights(employee.getDeptId())
            .execute(() -> {
                employeeMapper.updateById(employee);
                return null;
            });
    }
    
    // 带白名单的复杂场景
    public void updateWithWhitelist(Employee employee) {
        List<Long> adminDepts = getAdminDepartments();
        DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setValidRights(employee.getDeptId())
            .setWithoutRights(adminDepts) // 管理员部门绕过验证
            .execute(() -> {
                employeeMapper.updateById(employee);
                return null;
            });
    }
}
```

**应用数据权限前:**
```sql
SELECT * FROM employee
```

**应用数据权限后:**
```sql
SELECT * FROM employee t 
LEFT JOIN department d ON t.dept_id = d.id
WHERE d.id IN (10, 20, 30)
```

### SQL打印

库包含强大的 SQL 打印功能用于调试:

```properties
# application-dev.yml (非生产环境自动启用)
spring:
  profiles:
    active: dev
```

**控制台输出:**
```
=== SQL 执行 ===
方法: com.example.mapper.UserMapper.selectById
SQL: SELECT * FROM user WHERE id = 1 AND phone_number = '_sensitive_start_EnCrYpTeD123='
耗时: 15 ms
返回: 1 行数据
```

**功能特性:**
- ✅ 显示实际参数值(非占位符)
- ✅ 显示执行时间
- ✅ 显示影响/返回的行数
- ✅ 生产环境自动禁用(profile 包含 "prod")

## ⚙️ 配置说明

### 默认配置

库开箱即用,零配置。默认设置:

```yaml
# 这些是隐式默认值,无需配置
seven:
  data-security:
    encryption:
      algorithm: DES  # 默认加密算法
      key: "key12345678"  # 默认密钥(生产环境请更换!)
    sql-print:
      enabled: true  # 非生产环境启用
```

### 自定义配置

#### 应用配置

```yaml
spring:
  profiles:
    active: dev
  
# 自定义加密设置
seven:
  data-security:
    encryption:
      enabled: true
      algorithm: AES
```

#### 覆盖默认加密算法

```java
@Configuration
public class EncryptionConfig {
    
    @Bean
    @Primary  // 设为默认算法
    public EncryptionAlgo defaultEncryptionAlgo() {
        return new MyCustomEncryptionAlgo();
    }
}
```

#### 禁用 SQL 打印

```java
@Configuration
public class SqlPrintConfig {
    
    @Bean
    @ConditionalOnProperty(name = "sql.print.enabled", havingValue = "false")
    public ConfigurationCustomizer disableSqlPrint() {
        return configuration -> {
            // SQL 打印将不会配置
        };
    }
}
```

## 🎓 高级用法

### 嵌套对象加密

```java
@Data
public class Order {
    @TableId
    private Long id;
    
    // 嵌套对象加密
    private Customer customer;
    
    @Data
    public static class Customer {
        @EncryptField
        private String phone;
        
        @EncryptField
        private String email;
        
        private Address address;
        
        @Data
        public static class Address {
            @EncryptField
            private String street;
            
            private String city;
        }
    }
}
```

### 集合字段加密

```java
@Data
public class Company {
    @TableId
    private Long id;
    
    // 列表中的加密字段
    private List<Employee> employees;
    
    @Data
    public static class Employee {
        @EncryptField
        private String socialSecurityNumber;
        
        private String name;
    }
}
```

### 多级数据权限

```java
// 组合多个数据权限策略
public List<Document> getDocuments() {
    return DataScopeHelper
        .strategy(DepartmentDataScopeStrategy.class)
        .setSearchRight(userDeptIds)
        .execute(() -> 
            DataScopeHelper
                .strategy(ProjectDataScopeStrategy.class)
                .setSearchRight(userProjectIds)
                .execute(() -> documentMapper.selectAll())
        );
}
```

### JSON 序列化

库自动处理 Encrypt 类型序列化:

```java
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        // Encrypt 类型字段自动序列化为字符串
        return userService.getById(id);
    }
}
```

**JSON 输出:**
```json
{
  "id": 1,
  "username": "john",
  "phoneNumber": "13800138000",
  "email": "john@example.com"
}
```

## 🎨 设计模式

### 1. 策略模式
- **EncryptionAlgo 接口** - 可插拔的加密算法
- **DataScopeStrategy 接口** - 可定制的访问控制规则

### 2. 工厂模式
- **AbstractEncryptAlgoContainer** - 算法实例创建和缓存
- 懒加载与 Spring bean 集成

### 3. 模板方法模式
- **AbstractEncryptAlgoContainer** 提供算法解析模板
- 子类实现具体的默认算法逻辑

### 4. 单例模式
- **DecryptProcessor** 和 **EncryptProcessor** 使用 Holder 模式实现线程安全单例
- **SqlPrint** 工具使用内部 Holder 类

### 5. 拦截器模式
- **MyBatis Plugin** 机制实现透明加解密
- 责任链模式支持多个拦截器

### 6. 观察者模式
- **ObjectMapperEnhancer** 监听 `ContextRefreshedEvent` 配置 Jackson

### 7. 享元模式
- **字段元数据缓存** - `ClazzUtil` 减少反射开销
- **算法实例缓存** - 容器中缓存

## ⚡ 性能优化

### 缓存机制

1. **反射缓存**: 字段元数据缓存在 `ConcurrentHashMap`
2. **算法缓存**: 加密算法实例缓存
3. **表元数据缓存**: MyBatis-Plus 表信息缓存

### 优化建议

```java
// ✅ 推荐: 使用 @EncryptField 自动加密
@EncryptField
private String phoneNumber;

// ❌ 避免: 在业务逻辑中手动加密
private String phoneNumber;
public void setPhoneNumber(String phone) {
    this.phoneNumber = encryptionService.encrypt(phone); // 不推荐
}

// ✅ 推荐: 批量操作高效处理
userMapper.insertBatch(users); // 加密在单次拦截调用中完成

// ✅ 推荐: QueryWrapper 参数优化
new QueryWrapper<User>().in("phone_number", phoneList); // 批量加密

// ⚠️ 注意: 非敏感字段不加密
private String publicInfo; // 无 @EncryptField = 无开销
```

### 性能指标

- **加密开销**: 每字段约 1-2ms (DES 算法)
- **反射开销**: 首次访问后可忽略(已缓存)
- **SQL重写开销**: 数据权限应用 < 1ms

## 🧪 测试

### 单元测试示例

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    void testEncryption() {
        // 创建带敏感数据的用户
        User user = new User();
        user.setUsername("test");
        user.setPhoneNumber("13800138000");
        
        // 保存用户(自动加密)
        userService.createUser(user);
        
        // 直接从数据库查询验证加密
        User dbUser = userMapper.selectById(user.getId());
        
        // 电话号码应自动解密
        assertEquals("13800138000", dbUser.getPhoneNumber());
        
        // 验证数据库中是加密的
        String encryptedPhone = jdbcTemplate.queryForObject(
            "SELECT phone_number FROM user WHERE id = ?", 
            String.class, 
            user.getId()
        );
        assertTrue(encryptedPhone.startsWith("_sensitive_start_"));
    }
    
    @Test
    void testDataScope() {
        // 设置数据权限上下文
        List<Employee> employees = DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setSearchRight(Arrays.asList(10L, 20L))
            .execute(() -> employeeMapper.selectAll());
        
        // 验证只返回部门 10 和 20 的员工
        assertTrue(employees.stream()
            .allMatch(e -> Arrays.asList(10L, 20L).contains(e.getDeptId())));
    }
}
```

## 🤝 贡献指南

欢迎贡献!请遵循以下指南:

1. Fork 仓库
2. 创建特性分支: `git checkout -b feature/your-feature`
3. 提交更改: `git commit -am 'Add new feature'`
4. 推送分支: `git push origin feature/your-feature`
5. 提交 Pull Request

### 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/qwzhang01/seven-data-security.git
cd seven-data-security

# 构建项目
mvn clean install

# 运行测试
mvn test
```

## 📄 开源协议

本项目采用 MIT 协议 - 详见 [LICENSE](LICENSE) 文件

## 📞 联系方式

- **作者**: avinzhang
- **邮箱**: avinzhang@tencent.com
- **GitHub**: [https://github.com/qwzhang01/seven-data-security](https://github.com/qwzhang01/seven-data-security)
- **问题反馈**: [提交 Issue](https://github.com/qwzhang01/seven-data-security/issues)

## 🙏 致谢

- 感谢 Spring Boot 和 MyBatis 团队提供的优秀框架
- 感谢所有为本库做出贡献的开发者

## 📚 相关资源

- [MyBatis 文档](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

---

**如果这个项目对你有帮助,请给个 Star ⭐!**
