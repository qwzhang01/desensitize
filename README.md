# Spring boot及Mybatis下敏感字段页面脱敏、数据库加密解非侵入性实现方式

<br/>
&#8195;&#8195;&#8195;&#8195;系统开发中经常遇到保护用户敏感信息的需求，比如身份证、手机号码等，在页面显示需要脱敏，在数据库保存需要加密以防止脱库等。脱敏、加密解密属于与业务无关的公共逻辑，如果夹杂在业务代码里面，不仅会增加业务代码的复杂度，而且容易出错。
<br/>
<br/>
&#8195;&#8195;&#8195;&#8195;将其抽象提取到业务代码以外，使脱敏、加密解密对业务代码无侵入将能简化业务代码，降低出BUG的概率。
<br/>
<br/>

## 一、前端接口敏感字段脱敏

&#8195;&#8195;&#8195;&#8195;对于前端页面需要脱敏的字段特征分析：

1. 内存状态：在内存中是明文 
2. 脱敏时刻：给到前端页面那一刻脱敏 
3. 脱敏后是否还会用到该对象：脱敏后方法结束，无需再使用，即内存中的对象会被回收，即脱敏后内存状态无需关注
<br/>

&#8195;&#8195;&#8195;&#8195;基于以上特点分析，可以借助spring mvc的 ResponseBodyAdvice实现。ResponseBodyAdvice接口会对加了@RestController(也就是@Controller+@ResponseBody)注解的处理器的返回值进行增强处理，底层也是基于AOP实现的。
<br/>

&#8195;&#8195;&#8195;&#8195;ResponseBodyAdvice 有2个方法，一个是判断当前返回值是否需要增强处理，一个是实现增强处理的具体逻辑。
<br/>

&#8195;&#8195;&#8195;&#8195;基于脱敏的特点以及ResponseBodyAdvice 的实现方式，具体实现思路如下：
<br/>

1. 设计一个基类，基类只包含一个字段以说明当前业务状态是否需要脱敏。需要脱敏的实体都继承自该基类； 
2. 设计一个注解，注解只有一个参数说明脱敏算法类型（比如手机号码脱敏算法、邮箱脱敏算法等），需要脱敏的字段使用注解标注； 
3. 实现ResponseBodyAdvice 接口中的2个方法，通过返回值类型判断是否需要做增强处理；增强处理逻辑中根据反射获取父类判断是否需要脱敏，再通过反射获取脱敏注解的字段，将明文转换为密文。
<br/>

&#8195;&#8195;&#8195;&#8195;核心代码如下：
```
/**
 * 脱敏基类
 */
@Data
public class UnSensitiveDto implements Serializable {
    /**
     * 是否脱敏
     */
    private boolean sensitiveFlag = false;
}

```

```
/**
 * 脱敏注解
 *
 * @author avinzhang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnSensitive {
    /**
     * 标注不同的脱敏算法，比如邮箱脱敏算法、身份证号码脱敏算法、手机号码脱敏算法
     * @return
     */
    String type();
}
```

```
/**
 * 接口返回字段脱敏拦截器
 * 使用说明
 * <p>
 * 使用方法，返回结果的类继承 com.qw.desensitize.dto.UnSensitiveDto
 * com.qw.desensitize.dto.UnSensitiveDto#sensitiveFlag，脱敏的标识，比如本人登录状态，则赋值为false，不脱敏，其他人登录查看则赋值为true脱敏
 * <p>
 * 需要脱敏的字段添加注解 com.qw.desensitize.common.sensitive.UnSensitive
 * com.qw.desensitize.common.sensitive.UnSensitive#type() 为脱敏算法，目前实现了手机，身份证，邮箱三种脱敏算法，对应枚举定位位置 com.qw.desensitize.dto.UnSensitiveDto
 *
 * @author avinzhang
 */
@ControllerAdvice
@AllArgsConstructor
@Slf4j
public class UnSensitiveAdvice implements ResponseBodyAdvice<R> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Type type = returnType.getGenericParameterType();
        String typeName = type.getTypeName();
        return typeName.startsWith("com.qw.desensitize.common.R");
    }

    @Nullable
    @Override
    public R beforeBodyWrite(@Nullable R body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null) {
            if (body.getData() != null) {
                if (body.getData() instanceof UnSensitiveDto) {
                    UnSensitiveDto sensitive = (UnSensitiveDto) body.getData();
                    if (sensitive.isSensitiveFlag()) {
                        Long start = System.currentTimeMillis();
                        body.setData(unSensitive(sensitive));
                        log.warn("脱敏耗时{}毫秒", System.currentTimeMillis() - start);
                        return body;
                    }
                } else if (body.getData() instanceof List) {
                    List<Object> list = (List<Object>) body.getData();
                    if (list != null && list.size() > 0) {
                        Object element = list.get(0);
                        if (element instanceof UnSensitiveDto) {
                            UnSensitiveDto sensitive = (UnSensitiveDto) element;
                            if (sensitive.isSensitiveFlag()) {
                                Long start = System.currentTimeMillis();
                                body.setData(unSensitive(list));
                                log.warn("脱敏耗时{}毫秒", System.currentTimeMillis() - start);
                                return body;
                            }
                        }
                    }
                }
            }
        }
        return body;
    }

    private Object unSensitive(Object data) {
        try {
            if (data instanceof List) {
                // 处理list
                List<Object> list = (List) data;
                for (Object o : list) {
                    unSensitive(o);
                }
            } else {
                // 处理类
                unSensitiveParam(data);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    /**
     * 脱敏
     *
     * @param data
     * @throws IllegalAccessException
     */
    private void unSensitiveParam(Object data) throws IllegalAccessException {
        if (data == null) {
            return;
        }
        List<Field> fields = getFields(data.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> classType = field.getType();
            if (classType.getName().startsWith("com.qw.desensitize.dto")) {
                // 如果属性是自定义类，递归处理
                unSensitiveParam(field.get(data));
            } else if (List.class.isAssignableFrom(classType)) {
                Object objList = field.get(data);
                if (objList != null) {
                    List<Object> dataList = (List<Object>) objList;
                    for (Object dataParam : dataList) {
                        unSensitiveParam(dataParam);
                    }
                }
            } else {
                UnSensitive annotation = field.getAnnotation(UnSensitive.class);
                if (annotation != null) {
                    String type = annotation.type();
                    if (UN_SENSITIVE_EMAIL.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, email(String.valueOf(field.get(data))));
                        }
                    }
                    if (UN_SENSITIVE_PHONE.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, phone(String.valueOf(field.get(data))));
                        }
                    }
                    if (UnSensitiveDto.UN_SENSITIVE_ID_NUM.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, idNum(String.valueOf(field.get(data))));
                        }
                    }
                }
            }
        }
    }

    /**
     * 递归获取所有属性
     *
     * @param clazz
     * @return
     */
    private List<Field> getFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        list.addAll(Arrays.asList(declaredFields));


        Class<?> superclass = clazz.getSuperclass();
        if (superclass.getName().startsWith("com.qw.desensitize.dto")) {
            list.addAll(getFields(superclass));
        }
        return list;
    }

    /**
     * 脱敏邮箱
     *
     * @param src
     * @return
     */
    private String email(String src) {
        if (src == null) {
            return null;
        }
        String email = src.toString();
        int index = StringUtils.indexOf(email, "@");
        if (index <= 1) {
            return email;
        } else {
            return StringUtils.rightPad(StringUtils.left(email, 0), index, "*").concat(StringUtils.mid(email, index, StringUtils.length(email)));
        }
    }

    /**
     * 脱敏手机号码
     *
     * @param phone
     * @return
     */
    private String phone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return "";
        }
        return phone.replaceAll("(^\\d{0})\\d.*(\\d{4})", "$1****$2");
    }

    /**
     * 身份证脱敏
     *
     * @param idNumber
     * @return
     */
    private String idNum(String idNumber) {
        if (StringUtils.isBlank(idNumber)) {
            return "";
        }
        if (idNumber.length() == 15 || idNumber.length() == 18) {
            return idNumber.replaceAll("(\\w{4})\\w*(\\w{4})", "$1*********$2");
        }
        if (idNumber.length() > 4) {
            // 组织机构代码的方式脱敏****1111
            return idNumber.replaceAll("(\\w{0})\\w*(\\w{4})", "$1*********$2");
        }
        // 不足四位或者只有一位的都替代为*
        return "*********";
    }
}
```

```
/**
 * 继承父类，需要做脱敏处理
 */
@Data
public class UserDto extends UnSensitiveDto {
    private String name;
    /**
     * 对手机号码做脱敏的主机，脱敏算法是手机号码
     */
    @UnSensitive(type = UN_SENSITIVE_PHONE)
    private String phoneNo;
    private String gender;
    private Encrypt idNo;
}


/**
 * 控制器
 */
@GetMapping("info")
public R<UserDto> info(@RequestParam String userId) {
        // 获取数据库数据
        User user = mapper.selectById(userId);
        if (user == null) {
            return R.error();
        }
        // 转化为dto
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);

        // 标注需要脱敏
        dto.setSensitiveFlag(true);

        return R.success(dto);
    }
```

&#8195;&#8195;&#8195;&#8195;脱敏效果

![1.png](doc%2F1.png)


## 二、数据库敏感字段加密解密
&#8195;&#8195;&#8195;&#8195;对数据库字段加密解密特征分析：

<br/>

1. 加密时刻：入库的时候 
2. 解密时刻：出库的时候 
3. 内存状态要求：在内存中需是明文 
4. 解密后是否会用到对象：解密即读库，读库一定是为了使用，因此解密后内存需为明文 
5. 加密后是否会用到对象：某些业务入库即结束，有些用户入库后可能还有进一步的业务，因此加密后内存中仍需保持明文

&#8195;&#8195;&#8195;&#8195;基于以上特征分析，可以采用mybatis的Interceptor拦截器或新定义类型使用TypeHandler处理。
<br/>

#### 新定义类型通过TypeHandler处理思路
1. TypeHandler在mybatis中用于实现java类型和JDBC类型的相互转换。mybatis使用prepareStatement进行参数设置的时候，通过typeHandler将传入的java类型参数转换成对应的JDBC类型参数，这个过程是通过调用PrepareStatement不同的set方法实现的；在获取结果返回之后，需将返回的结果的JDBC类型转换成java类型，通过调用ResultSet对象不同类型的get方法实现；所以不同类型的typeHandler其实就是调用PrepareStatement和ResultSet的不同方法来进行类型的转换，因此可以在调用PrepareStatement和ResultSet的相关方法之前可以对传入的参数进行处理； 
2. 新定义一个类，代替需要加密的字符串； 
3. 继承BaseTypeHandler，实现setNonNullParameter方法实现加密逻辑，实现getNullableResult方法实现解密逻辑； 
4. 将与数据库交互的类中需要加密解密的字段类型用自定义的类代替； 
5. 如果需要对前端无感，可以统一对json工具做特殊处理，对加密对象的json序列化和反序列化按照字符串逻辑处理。

&#8195;&#8195;&#8195;&#8195;核心代码如下：
<br/>
```
/**
 * 加密字段字符串
 *
 * @author avinzhang
 */
public class Encrypt {
    private String value;

    public Encrypt(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Encrypt) {
            Encrypt objE = (Encrypt) obj;
            return value.equals(objE.getValue());
        }
        return false;
    }

    @Override
    public String toString() {
        return value;
    }
}
```

&#8195;&#8195;&#8195;&#8195;如果使用jackson进行json序列化和反序列化，可以通过新的序列化反序列化逻辑，代码如下：
<br/>
<br/>

```
// jackson
        ObjectMapper objectMapper = new ObjectMapper();


        // 加密解密字段json序列化与反序列化，按照字符串逻辑处理
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Encrypt.class, new JsonSerializer<Encrypt>() {
            @Override
            public void serialize(Encrypt value, JsonGenerator g, SerializerProvider serializers) throws IOException {
                g.writeString(value.getValue());
            }
        });
        simpleModule.addDeserializer(Encrypt.class, new JsonDeserializer<Encrypt>() {
            @Override
            public Encrypt deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                int currentTokenId = p.getCurrentTokenId();
                if (JsonTokenId.ID_STRING == currentTokenId) {
                    String text = p.getText().trim();
                    return new Encrypt(text);
                }
                throw new JacksonException("json 反序列化异常", "", Encrypt.class);
            }
        });
        objectMapper.registerModule(simpleModule);


```

```
import com.qw.desensitize.dto.Encrypt;
import com.qw.desensitize.kit.DesKit;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 加密解密类型转换器
 *
 * @author avinzhang
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(Encrypt.class)
public class EncryptTypeHandler extends BaseTypeHandler<Encrypt> {

    /**
     * 设置参数
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Encrypt parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.getValue() == null) {
            ps.setString(i, null);
            return;
        }
        String encrypt = DesKit.encrypt(DesKit.KEY, parameter.getValue());
        ps.setString(i, encrypt);
    }

    /**
     * 获取值
     */
    @Override
    public Encrypt getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    /**
     * 获取值
     */
    @Override
    public Encrypt getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    /**
     * 获取值
     */
    @Override
    public Encrypt getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    /**
     * 解密
     *
     * @param value
     * @return
     */
    private Encrypt decrypt(String value) {
        if (null == value) {
            return null;
        }
        return new Encrypt(DesKit.decrypt(DesKit.KEY, value));
    }
}
```
&#8195;&#8195;&#8195;&#8195;使用举例：
<br/>
<br/>
```
/**
 * 用户 entity
 */
@Data
@TableName("user")
@EncryptObj
public class User {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    @TableField("name")
    private String name;
    /**
     * 使用拦截器方式加密
     */
    @EncryptField
    @TableField("phoneNo")
    private String phoneNo;
    @TableField("gender")
    private String gender;
    /**
     * 使用类型转换器加密解密
     */
    @TableField("idNo")
    private Encrypt idNo;
}

/**
 * 用户dto
 * 继承父类，需要做脱敏处理
 */
@Data
public class UserDto extends UnSensitiveDto {
    private String name;
    /**
     * 对手机号码做脱敏的主机，脱敏算法是手机号码
     */
    @UnSensitive(type = UN_SENSITIVE_PHONE)
    private String phoneNo;
    private String gender;
    private Encrypt idNo;
}

    /**
     * 保存，保存后还需要使用
     * @param userDto
     * @return
     */
    @PostMapping("save-error")
    public R<UserDto> saveError(@RequestBody UserDto userDto) {
        // 保存
        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        mapper.insert(user);

        // 使用
        BeanUtils.copyProperties(user, userDto);
        return R.success(userDto);
    }
```
&#8195;&#8195;&#8195;&#8195;加密效果：
<br/>
<br/>
![2.png](doc%2F2.png)
![3.png](doc%2F3.png)

## 总结：
1. 页面字段脱敏利用spring mvc 的ResponseBodyAdvice，在返回结果前改变对象为密文 
2. 数据库加密解密，既可以采用mybatis的拦截器，也可以采用mybatis的类型转换器，拦截器在加密的同时会改变内存数据为密文，mybatis类型转换器不会改变原对象