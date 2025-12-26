# Seven Data Security

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.qwzhang01/seven-data-security.svg)](https://search.maven.org/artifact/io.github.qwzhang01/seven-data-security)
[![Java Version](https://img.shields.io/badge/Java-17%2B-green.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

[中文文档](README_ZH.md)

**Seven Data Security** is a comprehensive Spring Boot library for MyBatis that provides transparent data encryption, desensitization, and fine-grained data scope control for enterprise applications. It seamlessly integrates with MyBatis/MyBatis-Plus to automatically handle sensitive data protection without modifying business logic.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Core Functionality](#core-functionality)
  - [Field Encryption](#field-encryption)
  - [Query Parameter Encryption](#query-parameter-encryption)
  - [Data Scope Control](#data-scope-control)
  - [SQL Printing](#sql-printing)
- [Configuration](#configuration)
- [Advanced Usage](#advanced-usage)
- [Design Patterns](#design-patterns)
- [Performance Considerations](#performance-considerations)
- [Contributing](#contributing)
- [License](#license)

## ✨ Features

### 🔐 Automatic Data Encryption
- **Transparent encryption/decryption** via annotations
- **Multiple encryption algorithms** support (DES, AES, custom algorithms)
- **Zero business logic impact** - encryption handled by interceptors
- **Type-safe** Encrypt wrapper type for explicit encryption

### 🔍 Query Parameter Encryption
- **Automatic parameter detection** and encryption
- **Support for multiple parameter types**: Map, Object, MyBatis-Plus QueryWrapper
- **Smart field matching** with camelCase and snake_case conversion
- **Thread-safe parameter restoration** after SQL execution

### 🎯 Data Scope Control
- **Row-level data filtering** based on user permissions
- **Flexible strategy pattern** for custom access rules
- **SQL rewriting** with JOIN and WHERE conditions
- **Multi-tenant support** for SaaS applications

### 📊 Development Tools
- **SQL statement printing** with actual parameter values
- **Execution time tracking** for performance monitoring
- **Environment-aware** (disabled in production)

### 🏗️ Enterprise-Grade Design
- **Thread-safe operations** with ThreadLocal context
- **Lazy initialization** with caching for performance
- **Spring Boot auto-configuration**
- **Extensible architecture** via interfaces and abstract classes

## 🏛️ Architecture

```
seven-data-security
├── config                  # Auto-configuration classes
│   ├── JacksonConfig              # JSON serialization config
│   ├── MaskAutoConfig             # Main auto-config
│   └── MyBatisInterceptorAutoConfig  # MyBatis interceptors setup
├── domain                  # Core domain models
│   ├── AnnotatedField             # Field annotation metadata
│   ├── Encrypt                    # Encryption wrapper type
│   ├── ParameterEncryptInfo       # Parameter encryption context
│   └── ParameterRestoreInfo       # Parameter restoration context
├── encrypt                 # Encryption subsystem
│   ├── annotation                 # @EncryptField annotation
│   ├── container                  # Algorithm and metadata containers
│   ├── context                    # Encryption context management
│   ├── jackson                    # JSON serialization support
│   ├── processor                  # Encryption/decryption processors
│   ├── shield                     # Encryption algorithm implementations
│   └── type/handler               # MyBatis type handler
├── interceptor             # MyBatis interceptors
│   ├── DecryptInterceptor         # Result decryption
│   ├── SqlPrintInterceptor        # SQL logging
│   └── SqlRewriteInterceptor      # Parameter encryption & data scope
├── kit                     # Utility classes
│   ├── ClazzUtil                  # Reflection utilities
│   ├── FieldMatchUtil             # Field matching logic
│   ├── ParamUtil                  # Parameter processing
│   ├── SpringContextUtil          # Spring context access
│   ├── SqlPrint                   # SQL formatting
│   └── StringUtil                 # String operations
├── scope                   # Data scope subsystem
│   ├── DataScopeHelper            # Scope context management
│   ├── DataScopeStrategy          # Strategy interface
│   ├── container                  # Strategy container
│   └── processor                  # SQL rewriting processor
└── exception               # Exception hierarchy
    ├── DesensitizeException       # Base exception
    └── JacksonException           # JSON-related exception
```

## 🚀 Quick Start

### 1. Add Dependency

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

### 2. Define Entity with Encryption

```java
@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    
    private String username;
    
    // Automatically encrypted/decrypted
    @EncryptField
    private String phoneNumber;
    
    @EncryptField
    private String email;
    
    // Custom encryption algorithm
    @EncryptField(CustomAesAlgo.class)
    private String socialSecurityNumber;
}
```

### 3. Use in Service

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    // Data is automatically encrypted before insert
    public void createUser(User user) {
        userMapper.insert(user);
    }
    
    // Data is automatically decrypted after query
    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
    
    // Query parameters are automatically encrypted
    public List<User> findByPhone(String phone) {
        return userMapper.selectList(
            new QueryWrapper<User>().eq("phone_number", phone)
        );
    }
}
```

That's it! No additional code changes are required. The library automatically handles encryption and decryption.

## 🔑 Core Functionality

### Field Encryption

#### Using @EncryptField Annotation

```java
@Data
@TableName("customer")
public class Customer {
    @TableId
    private Long id;
    
    // Use default encryption algorithm
    @EncryptField
    private String phoneNumber;
    
    // Specify custom encryption algorithm
    @EncryptField(AesEncryptionAlgo.class)
    private String creditCard;
    
    private String name;
}
```

#### Using Encrypt Type Wrapper

```java
@Data
@TableName("sensitive_data")
public class SensitiveData {
    @TableId
    private Long id;
    
    // Type-safe encryption wrapper
    private Encrypt secretData;
    
    // Getter returns plain text
    public String getSecretData() {
        return secretData.getValue();
    }
    
    // Setter accepts plain text
    public void setSecretData(String value) {
        this.secretData = new Encrypt(value);
    }
}
```

#### Custom Encryption Algorithm

```java
public class AesEncryptionAlgo implements EncryptionAlgo {
    private static final String KEY = "YourSecretKey123"; // Use environment variable in production
    
    @Override
    public String encrypt(String value) {
        if (value == null) return null;
        // Implement AES encryption
        return AesUtils.encrypt(value, KEY);
    }
    
    @Override
    public String decrypt(String value) {
        if (value == null) return null;
        // Implement AES decryption
        return AesUtils.decrypt(value, KEY);
    }
}
```

#### Register Custom Algorithm

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public AesEncryptionAlgo aesEncryptionAlgo() {
        return new AesEncryptionAlgo();
    }
}
```

### Query Parameter Encryption

The library automatically encrypts query parameters that match encrypted fields:

```java
// All these queries automatically encrypt the phone parameter
public class UserService {
    
    // Map parameters
    public List<User> findByPhone(String phone) {
        Map<String, Object> params = new HashMap<>();
        params.put("phoneNumber", phone);
        return userMapper.selectByMap(params);
    }
    
    // Object parameters
    public List<User> findUsers(UserQuery query) {
        return userMapper.selectList(query); // query.phoneNumber is auto-encrypted
    }
    
    // QueryWrapper parameters (MyBatis-Plus)
    public List<User> findWithWrapper(String phone) {
        return userMapper.selectList(
            new QueryWrapper<User>().eq("phone_number", phone) // auto-encrypted
        );
    }
    
    // XML Mapper parameters
    public List<User> findInXml(String phone) {
        return userMapper.findByPhone(phone); // auto-encrypted
    }
}
```

**XML Mapper Example:**
```xml
<select id="findByPhone" resultType="User">
    SELECT * FROM user WHERE phone_number = #{phoneNumber}
    <!-- phoneNumber is automatically encrypted before execution -->
</select>
```

### Data Scope Control

Implement fine-grained data access control based on user permissions:

#### 1. Define Data Scope Strategy

```java
@Component
public class DepartmentDataScopeStrategy implements DataScopeStrategy<Long> {
    
    @Autowired
    private UserContext userContext;
    
    @Override
    public String join() {
        // Add JOIN clause if needed
        return "LEFT JOIN department d ON t.dept_id = d.id";
    }
    
    @Override
    public String where() {
        // Add WHERE clause for permission filtering
        List<Long> deptIds = userContext.getUserDepartmentIds();
        return "d.id IN (" + StringUtils.join(deptIds, ",") + ")";
    }
    
    @Override
    public void validDs(List<Long> validRights) {
        // Validate permissions for INSERT/UPDATE/DELETE
        Long currentDeptId = userContext.getCurrentDepartmentId();
        if (!validRights.contains(currentDeptId)) {
            throw new PermissionDeniedException("No permission for this department");
        }
    }
    
    @Override
    public void validDs(List<Long> validRights, List<Long> withoutRights) {
        // Whitelist support: if in whitelist, skip validation
        Long currentDeptId = userContext.getCurrentDepartmentId();
        if (withoutRights.contains(currentDeptId)) {
            return;
        }
        validDs(validRights);
    }
}
```

#### 2. Apply Data Scope to Query

```java
@Service
public class EmployeeService {
    
    @Autowired
    private EmployeeMapper employeeMapper;
    
    // Apply department-level data scope
    public List<Employee> getEmployees() {
        return DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setSearchRight(getCurrentUserDeptIds())
            .execute(() -> employeeMapper.selectAll());
    }
    
    // Apply with validation for update
    public void updateEmployee(Employee employee) {
        DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setValidRights(employee.getDeptId())
            .execute(() -> {
                employeeMapper.updateById(employee);
                return null;
            });
    }
    
    // Complex scenario with whitelist
    public void updateWithWhitelist(Employee employee) {
        List<Long> adminDepts = getAdminDepartments();
        DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setValidRights(employee.getDeptId())
            .setWithoutRights(adminDepts) // Admin depts bypass validation
            .execute(() -> {
                employeeMapper.updateById(employee);
                return null;
            });
    }
}
```

**Before Data Scope:**
```sql
SELECT * FROM employee
```

**After Data Scope Applied:**
```sql
SELECT * FROM employee t 
LEFT JOIN department d ON t.dept_id = d.id
WHERE d.id IN (10, 20, 30)
```

### SQL Printing

The library includes a powerful SQL printing feature for debugging:

```properties
# application-dev.yml (automatically enabled in non-prod environments)
spring:
  profiles:
    active: dev
```

**Console Output:**
```
=== SQL Execution ===
Method: com.example.mapper.UserMapper.selectById
SQL: SELECT * FROM user WHERE id = 1 AND phone_number = '_sensitive_start_EnCrYpTeD123='
Time: 15 ms
Returned: 1 rows
```

**Features:**
- ✅ Shows actual parameter values (not placeholders)
- ✅ Displays execution time
- ✅ Shows affected/returned row counts
- ✅ Automatically disabled in production (profile containing "prod")

## ⚙️ Configuration

### Default Configuration

The library works out of the box with zero configuration. Default settings:

```yaml
# These are implicit defaults, no need to configure
seven:
  data-security:
    encryption:
      algorithm: DES  # Default encryption algorithm
      key: "key12345678"  # Default key (change in production!)
    sql-print:
      enabled: true  # Enabled in non-production environments
```

### Custom Configuration

#### Application Properties

```yaml
spring:
  profiles:
    active: dev
  
# Custom encryption settings
seven:
  data-security:
    encryption:
      enabled: true
      algorithm: AES
```

#### Override Default Encryption Algorithm

```java
@Configuration
public class EncryptionConfig {
    
    @Bean
    @Primary  // Make this the default algorithm
    public EncryptionAlgo defaultEncryptionAlgo() {
        return new MyCustomEncryptionAlgo();
    }
}
```

#### Disable SQL Printing

```java
@Configuration
public class SqlPrintConfig {
    
    @Bean
    @ConditionalOnProperty(name = "sql.print.enabled", havingValue = "false")
    public ConfigurationCustomizer disableSqlPrint() {
        return configuration -> {
            // SQL printing will not be configured
        };
    }
}
```

## 🎓 Advanced Usage

### Nested Object Encryption

```java
@Data
public class Order {
    @TableId
    private Long id;
    
    // Nested object encryption
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

### Collection Field Encryption

```java
@Data
public class Company {
    @TableId
    private Long id;
    
    // List of encrypted fields
    private List<Employee> employees;
    
    @Data
    public static class Employee {
        @EncryptField
        private String socialSecurityNumber;
        
        private String name;
    }
}
```

### Multi-Level Data Scope

```java
// Combine multiple data scope strategies
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

### JSON Serialization

The library automatically handles Encrypt type serialization:

```java
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        // Encrypt type fields are automatically serialized as plain strings
        return userService.getById(id);
    }
}
```

**JSON Output:**
```json
{
  "id": 1,
  "username": "john",
  "phoneNumber": "13800138000",
  "email": "john@example.com"
}
```

## 🎨 Design Patterns

### 1. Strategy Pattern
- **EncryptionAlgo interface** for pluggable encryption algorithms
- **DataScopeStrategy interface** for customizable access control rules

### 2. Factory Pattern
- **AbstractEncryptAlgoContainer** for algorithm instance creation and caching
- Lazy initialization with Spring bean integration

### 3. Template Method Pattern
- **AbstractEncryptAlgoContainer** provides template for algorithm resolution
- Subclasses implement specific default algorithm logic

### 4. Singleton Pattern
- **DecryptProcessor** and **EncryptProcessor** use Holder pattern for thread-safe singletons
- **SqlPrint** utility uses inner Holder class

### 5. Interceptor Pattern
- **MyBatis Plugin** mechanism for transparent encryption/decryption
- Chain of responsibility for multiple interceptors

### 6. Observer Pattern
- **ObjectMapperEnhancer** listens to `ContextRefreshedEvent` for Jackson configuration

### 7. Flyweight Pattern
- **Field metadata caching** in `ClazzUtil` reduces reflection overhead
- **Algorithm instance caching** in containers

## ⚡ Performance Considerations

### Caching Mechanisms

1. **Reflection Cache**: Field metadata cached in `ConcurrentHashMap`
2. **Algorithm Cache**: Encryption algorithm instances cached
3. **Table Metadata Cache**: MyBatis-Plus table info cached

### Optimization Tips

```java
// ✅ Good: Use @EncryptField for automatic encryption
@EncryptField
private String phoneNumber;

// ❌ Avoid: Manual encryption in business logic
private String phoneNumber;
public void setPhoneNumber(String phone) {
    this.phoneNumber = encryptionService.encrypt(phone); // Not recommended
}

// ✅ Good: Batch operations are efficiently handled
userMapper.insertBatch(users); // Encryption happens in single intercept call

// ✅ Good: QueryWrapper parameters are optimized
new QueryWrapper<User>().in("phone_number", phoneList); // Batch encryption

// ⚠️ Note: Disable encryption for non-sensitive fields
private String publicInfo; // No @EncryptField = No overhead
```

### Performance Metrics

- **Encryption Overhead**: ~1-2ms per field (DES algorithm)
- **Reflection Overhead**: Negligible after first access (cached)
- **SQL Rewrite Overhead**: < 1ms for data scope application

## 🧪 Testing

### Unit Test Example

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    void testEncryption() {
        // Create user with sensitive data
        User user = new User();
        user.setUsername("test");
        user.setPhoneNumber("13800138000");
        
        // Save user (encryption happens automatically)
        userService.createUser(user);
        
        // Query directly from database to verify encryption
        User dbUser = userMapper.selectById(user.getId());
        
        // Phone number should be decrypted automatically
        assertEquals("13800138000", dbUser.getPhoneNumber());
        
        // Verify data is encrypted in database
        String encryptedPhone = jdbcTemplate.queryForObject(
            "SELECT phone_number FROM user WHERE id = ?", 
            String.class, 
            user.getId()
        );
        assertTrue(encryptedPhone.startsWith("_sensitive_start_"));
    }
    
    @Test
    void testDataScope() {
        // Set up data scope context
        List<Employee> employees = DataScopeHelper
            .strategy(DepartmentDataScopeStrategy.class)
            .setSearchRight(Arrays.asList(10L, 20L))
            .execute(() -> employeeMapper.selectAll());
        
        // Verify only employees from departments 10 and 20 are returned
        assertTrue(employees.stream()
            .allMatch(e -> Arrays.asList(10L, 20L).contains(e.getDeptId())));
    }
}
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Submit a pull request

### Development Setup

```bash
# Clone repository
git clone https://github.com/qwzhang01/seven-data-security.git
cd seven-data-security

# Build project
mvn clean install

# Run tests
mvn test
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact & Support

- **Author**: avinzhang
- **Email**: avinzhang@tencent.com
- **GitHub**: [https://github.com/qwzhang01/seven-data-security](https://github.com/qwzhang01/seven-data-security)
- **Issues**: [Report Issues](https://github.com/qwzhang01/seven-data-security/issues)

## 🙏 Acknowledgments

- Thanks to the Spring Boot and MyBatis teams for their excellent frameworks
- Thanks to all contributors who have helped improve this library

## 📚 Additional Resources

- [MyBatis Documentation](https://mybatis.org/mybatis-3/)
- [MyBatis-Plus Documentation](https://baomidou.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

**Star ⭐ this repository if you find it helpful!**
