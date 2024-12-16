package com.charity_hub.infrastructure.repositories;

import com.charity_hub.infrastructure.dtos.AccountEventDto;
import com.charity_hub.domain.contracts.IAccountRepo;
import com.charity_hub.domain.events.AccountEvent;
import com.charity_hub.domain.model.account.Account;
import com.charity_hub.infrastructure.db.AccountEntity;
import com.charity_hub.infrastructure.db.RevokedAccountEntity;
import com.charity_hub.shared.domain.IEventBus;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.*;

@Repository
public class AccountRepo implements IAccountRepo {
    private static final String ACCOUNTS_COLLECTION = "accounts";
    private static final String REVOKED_ACCOUNT_COLLECTION = "revoked_accounts";

    private final MongoCollection<AccountEntity> collection;
    private final List<String> admins;
    private final IEventBus eventBus;

    public AccountRepo(
            @Value("${accounts.admins}") List<String> admins,
            MongoDatabase mongoDatabase,
            IEventBus eventBus
    ) {
        this.admins = admins;
        this.collection = mongoDatabase.getCollection(ACCOUNTS_COLLECTION, AccountEntity.class);
        mongoDatabase.getCollection(REVOKED_ACCOUNT_COLLECTION, RevokedAccountEntity.class);
        this.eventBus = eventBus;
    }

    @Override
    public CompletableFuture<Account> getByMobileNumber(String mobileNumber) {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(collection.find(eq("mobileNumber", mobileNumber)).first())
                        .map(AccountEntity::toDomain)
                        .orElse(null)
        );
    }

    @Override
    public CompletableFuture<Void> save(Account account) {
        return CompletableFuture.supplyAsync(() -> {
            var entity = AccountEntity.from(account);
            collection.replaceOne(
                    eq("accountId", entity.accountId()),
                    entity,
                    new ReplaceOptions().upsert(true)
            );
            account.occurredEvents().stream()
                    .map(event -> AccountEventDto.from((AccountEvent) event))
                    .forEach(eventBus::push);
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> isAdmin(String mobileNumber) {
        return CompletableFuture.completedFuture(admins.contains(mobileNumber));
    }
}