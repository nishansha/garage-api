package com.triasoft.garage.constants;


public enum ClientChannel {
    WEB, MOBILE;

    public static ClientChannel fromHeader(String value) {
        if (value == null) {
            return null;
        }
        for (ClientChannel channel : values()) {
            if (channel.name().equalsIgnoreCase(value.trim())) {
                return channel;
            }
        }
        return null;
    }
}
