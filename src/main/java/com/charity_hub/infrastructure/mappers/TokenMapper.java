package com.charity_hub.infrastructure.mappers;

import com.charity_hub.domain.model.account.Account;
import com.charity_hub.domain.model.device.Device;
import com.charity_hub.shared.api.auth.AccessTokenPayload;
import com.charity_hub.shared.api.auth.JWTPayload;
import com.charity_hub.shared.api.auth.RefreshTokenPayload;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class TokenMapper {

    public static JWTPayload toAccessToken(Account account, Device device, Date expireAt) {
        Logger.getAnonymousLogger().info("jwt id " + UUID.randomUUID());

        return new AccessTokenPayload(
                device.getDeviceType().value(),           // audience
                UUID.randomUUID().toString(),             // jwtId
                expireAt,                                 // expireAt
                new Date(),                               // issuedAt
                account.getId().value().toString(),       // uuid
                account.getFullName() != null ?           // fullName
                        account.getFullName().value() : null,
                account.getPhotoUrl() != null ?           // photoUrl
                        account.getPhotoUrl().value() : null,
                account.isBlocked(),                      // blocked
                account.getMobileNumber().value(),        // mobileNumber
                device.getDeviceId().value(),             // deviceId
                account.getPermissions().stream()         // permissions
                        .map(Enum::name)
                        .collect(Collectors.toList())
        );
    }

    public static JWTPayload toRefreshToken(Account account, Device device, Date expireAt) {
        return new RefreshTokenPayload(
                device.getDeviceType().value(),           // audience
                UUID.randomUUID().toString(),             // jwtId
                expireAt,                                 // expireAt
                new Date(),                               // issuedAt
                account.getId().value().toString(),       // uuid
                account.getMobileNumber().value(),        // mobileNumber
                device.getDeviceId().value()             // deviceId
        );
    }
}