package com.charity_hub.application;

import com.charity_hub.domain.contracts.IAccountRepo;
import com.charity_hub.domain.contracts.IAuthProvider;
import com.charity_hub.domain.contracts.IInvitationRepo;
import com.charity_hub.domain.contracts.IJWTGenerator;
import com.charity_hub.domain.model.account.Tokens;
import com.charity_hub.domain.model.account.Account;
import com.charity_hub.shared.abstractions.CommandHandler;
import com.charity_hub.shared.exceptions.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AuthenticateHandler extends CommandHandler<Authenticate, AuthenticateResponse> {
    private final IAccountRepo accountRepo;
    private final IInvitationRepo invitationRepo;
    private final IAuthProvider authProvider;
    private final IJWTGenerator jwtGenerator;

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public AuthenticateHandler(
            IAccountRepo accountRepo,
            IInvitationRepo invitationRepo,
            IAuthProvider authProvider,
            IJWTGenerator jwtGenerator
    ) {
        this.accountRepo = accountRepo;
        this.invitationRepo = invitationRepo;
        this.authProvider = authProvider;
        this.jwtGenerator = jwtGenerator;
    }

    @Override
    public CompletableFuture<AuthenticateResponse> handle(Authenticate command) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Handling authentication for idToken: {}", command.idToken());

            String mobileNumber = authProvider.getVerifiedMobileNumber(command.idToken()).join();

            logger.info("check for account: {}", command);

            Account account = existingAccountOrNewAccount(mobileNumber, command);

            logger.info(" finish check for account: {}", command.idToken());

            Tokens tokens = account.authenticate(jwtGenerator, command.deviceId(), command.deviceType());

            accountRepo.save(account);
            logger.info("Authentication successful for account: {}", account.getMobileNumber());

            return new AuthenticateResponse(tokens.accessToken(), tokens.refreshToken());
        });
    }

    private Account existingAccountOrNewAccount(String mobileNumber, Authenticate request) {
        logger.info("get the account by mobile number");
        var account = accountRepo.getByMobileNumber(mobileNumber).join();
        if (accountExists(account)) {
            return account;
        }
        return createNewAccount(mobileNumber, request.deviceType(), request.deviceId());
    }

    private static boolean accountExists(Account existingAccount) {
        return existingAccount != null;
    }

    private Account createNewAccount(String mobileNumber, String aDeviceType, String aDeviceId) {
        boolean isAdmin = accountRepo.isAdmin(mobileNumber).join();
        boolean hasNoInvitations = !invitationRepo.hasInvitation(mobileNumber).join();
        assertIsAdminOrInvited(mobileNumber, isAdmin, hasNoInvitations);

        return Account.newAccount(mobileNumber, isAdmin, aDeviceType, aDeviceId);
    }

    private void assertIsAdminOrInvited(String mobileNumber, boolean isAdmin, boolean hasNoInvitations) {
        if (!isAdmin && hasNoInvitations) {
            logger.warn("Account not invited: {}", mobileNumber);
            throw new AppException.RequirementException("Account not invited to use the App");
        }
    }
}