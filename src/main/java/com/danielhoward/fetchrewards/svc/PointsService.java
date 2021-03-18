package com.danielhoward.fetchrewards.svc;

import com.danielhoward.fetchrewards.svcdata.Transaction;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PointsService {

    private final CacheManager cacheManager;

    private static final String TRANSACTIONS_CACHE = "transactions";

    public PointsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<Transaction> postTransaction(Transaction transaction) {
        Cache cache = cacheManager.getCache(TRANSACTIONS_CACHE);
        String payerName = transaction.getPayer();
        if (payerName == null) {
            throw new IllegalArgumentException("Payer field of request cannot be null.");
        }
        if (cache != null) {
            List<Transaction> existingTransactions = (List<Transaction>) Optional.ofNullable(cache.get(payerName))
                    .map(Cache.ValueWrapper::get)
                    .orElse(new ArrayList());
            existingTransactions.add(transaction);
            List<Transaction> sortedTransactions = existingTransactions.stream()
                    .sorted(Comparator.comparing(Transaction::getTimestamp))
                    .collect(Collectors.toList());
            cache.put(payerName, sortedTransactions);
            return sortedTransactions;
        }
        return List.of();
    }

}
