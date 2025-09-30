# Data Desensitization Library

A comprehensive Java library for data masking and encryption, designed to protect sensitive information in applications. This library provides flexible and configurable data masking strategies for various types of sensitive data including phone numbers, ID cards, emails, and personal names.

## Features

- **Multiple Masking Algorithms**: Support for phone numbers, ID cards, emails, Chinese names, and English names
- **Encryption Support**: Built-in DES encryption for reversible data protection
- **Spring Boot Integration**: Auto-configuration for seamless Spring Boot integration
- **Flexible Configuration**: Easy to customize and extend with your own algorithms
- **Thread-Safe**: All operations are thread-safe for concurrent environments
- **Annotation-Based**: Support for annotation-driven masking (coming soon)

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.qwzhang01</groupId>
    <artifactId>desensitize-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

#### Using Default Masking Algorithms

```java
import io.github.qwzhang01.desensitize.shield.DefaultCoverAlgo;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;

// Create an instance of the default masking algorithm
CoverAlgo coverAlgo = new DefaultCoverAlgo();

// Mask phone number
String maskedPhone = coverAlgo.maskPhone("13812345678");
// Result: "138****5678"

// Mask ID card
String maskedId = coverAlgo.maskIdCard("110101199001011234");
// Result: "110101********1234"

// Mask email
String maskedEmail = coverAlgo.maskEmail("example@gmail.com");
// Result: "e****e@gmail.com"

// Mask Chinese name
String maskedChineseName = coverAlgo.maskChineseName("张三丰");
// Result: "张*丰"

// Mask English name
String maskedEnglishName = coverAlgo.maskEnglishName("John");
// Result: "J**n"
```

#### Using Encryption

```java
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

// Create an instance of the default encryption algorithm
EncryptionAlgo encryptionAlgo = new DefaultEncryptionAlgo();

// Encrypt sensitive data
String encrypted = encryptionAlgo.encrypt("sensitive data");

// Decrypt data
String decrypted = encryptionAlgo.decrypt(encrypted);
```

### Spring Boot Integration

The library provides auto-configuration for Spring Boot applications. Simply add the dependency and the beans will be automatically configured.

#### Using Spring Context Utility

```java
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;

// Get masking algorithm from Spring context
CoverAlgo coverAlgo = SpringContextUtil.getBean(CoverAlgo.class);

// Use the algorithm
String masked = coverAlgo.maskPhone("13812345678");
```

#### Using Utility Class

```java
import io.github.qwzhang01.desensitize.kit.DesensitizeUtil;

// Direct static method calls (automatically uses Spring beans if available)
String maskedPhone = DesensitizeUtil.maskPhone("13812345678");
String maskedEmail = DesensitizeUtil.maskEmail("user@example.com");
String encrypted = DesensitizeUtil.encrypt("sensitive data");
```

## Masking Strategies

### Phone Number Masking
- **Pattern**: Keeps first 3 and last 4 digits, masks middle 4 digits
- **Example**: `13812345678` → `138****5678`
- **Validation**: Only processes valid Chinese mobile phone numbers

### ID Card Masking
- **18-digit ID**: Keeps first 6 and last 4 digits, masks middle 8 digits (birth date)
- **15-digit ID**: Keeps first 6 and last 3 digits, masks middle 6 digits
- **Example**: `110101199001011234` → `110101********1234`

### Email Masking
- **Pattern**: Keeps first and last character of username, masks middle part
- **Example**: `example@gmail.com` → `e****e@gmail.com`
- **Domain**: Domain part remains unchanged

### Chinese Name Masking
- **2 characters**: Keeps family name, masks given name
- **3+ characters**: Keeps first and last character, masks middle
- **Examples**: 
  - `张三` → `张*`
  - `张三丰` → `张*丰`

### English Name Masking
- **Pattern**: Keeps first and last character, masks middle
- **Example**: `John` → `J**n`

## Configuration

### Custom Masking Algorithm

```java
import io.github.qwzhang01.desensitize.shield.CoverAlgo;

@Component
public class CustomCoverAlgo implements CoverAlgo {
    @Override
    public String mask(String content) {
        // Your custom masking logic
        return "***";
    }
    
    // You can also override specific methods from RoutineCoverAlgo
}
```

### Custom Encryption Algorithm

```java
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

@Component
public class CustomEncryptionAlgo implements EncryptionAlgo {
    @Override
    public String encrypt(String value) {
        // Your custom encryption logic
        return encryptedValue;
    }
    
    @Override
    public String decrypt(String value) {
        // Your custom decryption logic
        return decryptedValue;
    }
}
```

## Auto-Configuration

The library automatically configures the following beans when no custom implementations are provided:

- `CoverAlgo`: Default data masking algorithm
- `EncryptionAlgo`: Default DES encryption algorithm  
- `SpringContextUtil`: Utility for accessing Spring beans

### Disabling Auto-Configuration

```java
@SpringBootApplication(exclude = {MaskAutoConfig.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Security Considerations

### Encryption Algorithm
- The default implementation uses DES encryption, which is considered weak by modern standards
- For production environments, consider implementing a custom `EncryptionAlgo` with stronger algorithms like AES
- Always use secure key management practices

### Key Management
- The default encryption key is hardcoded for demonstration purposes
- In production, externalize keys using environment variables or secure key management systems
- Rotate encryption keys regularly

### Data Handling
- Masked data is not encrypted and should not be considered secure for storage
- Use encryption for data that needs to be reversible
- Use masking for display and logging purposes

## Examples

### Complete Spring Boot Example

```java
@RestController
public class UserController {
    
    @Autowired
    private CoverAlgo coverAlgo;
    
    @Autowired
    private EncryptionAlgo encryptionAlgo;
    
    @GetMapping("/user/{id}")
    public UserDto getUser(@PathVariable String id) {
        User user = userService.findById(id);
        
        // Mask sensitive data for response
        UserDto dto = new UserDto();
        dto.setPhone(coverAlgo.maskPhone(user.getPhone()));
        dto.setEmail(coverAlgo.maskEmail(user.getEmail()));
        dto.setIdCard(coverAlgo.maskIdCard(user.getIdCard()));
        
        return dto;
    }
    
    @PostMapping("/user")
    public void createUser(@RequestBody CreateUserRequest request) {
        User user = new User();
        
        // Encrypt sensitive data before storage
        user.setPhone(encryptionAlgo.encrypt(request.getPhone()));
        user.setIdCard(encryptionAlgo.encrypt(request.getIdCard()));
        
        userService.save(user);
    }
}
```

### Utility Class Example

```java
public class DataProcessor {
    
    public void processUserData(String phone, String email, String name) {
        // Using static utility methods
        String maskedPhone = DesensitizeUtil.maskPhone(phone);
        String maskedEmail = DesensitizeUtil.maskEmail(email);
        String maskedName = DesensitizeUtil.maskChineseName(name);
        
        // Log masked data safely
        log.info("Processing user: phone={}, email={}, name={}", 
                maskedPhone, maskedEmail, maskedName);
        
        // Encrypt for storage
        String encryptedPhone = DesensitizeUtil.encrypt(phone);
        // Store encrypted data...
    }
}
```

## API Reference

### CoverAlgo Interface

| Method | Description | Example Input | Example Output |
|--------|-------------|---------------|----------------|
| `mask(String)` | Generic masking method | `"sensitive"` | `"*****"` |
| `maskPhone(String)` | Mask phone number | `"13812345678"` | `"138****5678"` |
| `maskIdCard(String)` | Mask ID card number | `"110101199001011234"` | `"110101********1234"` |
| `maskEmail(String)` | Mask email address | `"user@example.com"` | `"u**r@example.com"` |
| `maskChineseName(String)` | Mask Chinese name | `"张三丰"` | `"张*丰"` |
| `maskEnglishName(String)` | Mask English name | `"John"` | `"J**n"` |

### EncryptionAlgo Interface

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `encrypt(String)` | Encrypt plain text | Plain text string | Encrypted string (Base64) |
| `decrypt(String)` | Decrypt encrypted text | Encrypted string (Base64) | Plain text string |

### SpringContextUtil Class

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getBean(Class<T>)` | Get bean by type | Bean class | Bean instance |
| `getBean(String)` | Get bean by name | Bean name | Bean instance |
| `getBeanSafely(Class<T>)` | Safely get bean by type | Bean class | Bean instance or null |
| `containsBean(String)` | Check if bean exists | Bean name | boolean |
| `isInitialized()` | Check if context is ready | None | boolean |

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup

1. Clone the repository
2. Import into your IDE as a Maven project
3. Run tests: `mvn test`
4. Build: `mvn clean package`

### Code Style

- Follow standard Java coding conventions
- Add comprehensive JavaDoc comments
- Include unit tests for new features
- Ensure thread safety for all public methods

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### MIT License

```
MIT License

Copyright (c) 2024 avinzhang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Changelog

### Version 1.0.0
- Initial release
- Basic masking algorithms for phone, ID card, email, and names
- DES encryption support
- Spring Boot auto-configuration
- Spring context utility

## Support

If you encounter any issues or have questions, please:

1. Check the [documentation](#api-reference)
2. Search existing [issues](https://github.com/qwzhang01/desensitize-lib/issues)
3. Create a new issue with detailed information

## Roadmap

- [ ] Annotation-based masking support
- [ ] AES encryption algorithm
- [ ] Custom masking patterns
- [ ] Performance optimizations
- [ ] Additional data type support
- [ ] Internationalization support