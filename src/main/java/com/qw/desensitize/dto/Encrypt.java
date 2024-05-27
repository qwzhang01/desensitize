package com.qw.desensitize.dto;

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