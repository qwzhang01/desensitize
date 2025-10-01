package io.github.qwzhang01.desensitize.exception;

/**
 * JSON 序列号华反序列化异常
 *
 * @author avinzhang
 */
public class JacksonException extends DesensitizeException {
    private String json;
    private Class<?> clazz;

    public JacksonException(String message, String json, Class<?> clazz) {
        super(message);
        this.json = json;
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}