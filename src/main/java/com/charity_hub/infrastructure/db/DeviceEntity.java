package com.charity_hub.infrastructure.db;


import com.charity_hub.domain.model.device.Device;

public record DeviceEntity(String deviceId,
                           String deviceType,
                           String refreshToken,
                           String fcmToken,
                           long lastAccessTime) {
    public static DeviceEntity from(Device domain) {
        return new DeviceEntity(
                domain.getDeviceId().value(),
                domain.getDeviceType().value(),
                domain.getRefreshToken() != null ? domain.getRefreshToken().getValue() : null,
                domain.getFcmToken() != null ? domain.getFcmToken().getValue() : null,
                domain.getLastAccessTime().getTime()
        );
    }

    public Device toDomain() {
        return Device.create(
                deviceId(),
                deviceType(),
                refreshToken(),
                fcmToken(),
                lastAccessTime()
        );
    }
}