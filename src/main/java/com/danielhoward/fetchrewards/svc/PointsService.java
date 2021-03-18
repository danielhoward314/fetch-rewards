package com.danielhoward.fetchrewards.svc;

import com.danielhoward.fetchrewards.svcdata.PointsSpendEntry;
import com.danielhoward.fetchrewards.svcdata.PointsUsageSummary;
import com.danielhoward.fetchrewards.svcdata.Transaction;
import com.danielhoward.fetchrewards.svcdata.UpdateLedger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PointsService {

    private final CacheManager cacheManager;

    private static final String BALANCES_CACHE = "balances";

    private static final String TRANSACTIONS_CACHE = "transactions";

    public PointsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void postTransaction(Transaction transaction) {
        CaffeineCache trxCache = (CaffeineCache) cacheManager.getCache(TRANSACTIONS_CACHE);
        String payerName = transaction.getPayer();
        if (payerName == null || transaction.getPoints() == null || transaction.getTimestamp() == null) {
            throw new IllegalArgumentException("A required field of request is null.");
        }
        if (trxCache != null) {
            List<Transaction> existingTransactions = (List<Transaction>) Optional.ofNullable(trxCache.get(payerName))
                .map(Cache.ValueWrapper::get)
                .orElse(new ArrayList());
            existingTransactions.add(transaction);
            trxCache.put(payerName, existingTransactions);
            CaffeineCache balancesCache = (CaffeineCache) cacheManager.getCache(BALANCES_CACHE);
            Long balance = existingTransactions.stream()
                .map(Transaction::getPoints)
                .reduce(0L, Long::sum);
            balancesCache.put(payerName, balance);
        }
    }

    public List<PointsUsageSummary> spendPoints(PointsSpendEntry spendEntry) {
        List<PointsUsageSummary> pointsUsageSummaries = new ArrayList<>();
        Cache trxCache = cacheManager.getCache(TRANSACTIONS_CACHE);
        Set<Object> allTrxKeys = getAllKeys(TRANSACTIONS_CACHE);
        Cache balancesCache = cacheManager.getCache(BALANCES_CACHE);
        // Merges all payer transactions into sorted list to enforce earliest timestamp rule.
        List<Transaction> allTransactions = getAllTransactionsSorted(trxCache, allTrxKeys);
        Long pointsToCover = spendEntry.getPoints();
        Map<String, UpdateLedger> pointsUsageByPayer = new HashMap<>();
        int i = 0;
        while (i < allTransactions.size()) {
            Transaction current = allTransactions.get(i);
            UpdateLedger updatesSoFar = pointsUsageByPayer.getOrDefault(
                current.getPayer(),
                new UpdateLedger(new ArrayList<>(), 0L)
            );
            Long savedPayerPoints = updatesSoFar.getPointsSpent();
            Long payerPointsUsed;
            if (current.getPoints() < 0) {
                payerPointsUsed = savedPayerPoints + current.getPoints();
                updatesSoFar.getIndicesToRemove().add(i);
                updatesSoFar.setPointsSpent(payerPointsUsed);
                pointsUsageByPayer.put(current.getPayer(), updatesSoFar);
            } else {
                Long currentBalance = (Long) Optional.ofNullable(balancesCache.get(current.getPayer()))
                    .map(Cache.ValueWrapper::get)
                    .orElse(0L);
                Long ptsAvailable;
                // Enforce rule that prohibits payer balance going negative.
                if ((currentBalance - current.getPoints()) < 0) {
                    ptsAvailable = current.getPoints() - Math.abs(currentBalance - current.getPoints());
                } else {
                    ptsAvailable = current.getPoints();
                }
                // Use only as many points as are needed to cover the spend request.
                if ((pointsToCover - ptsAvailable) < 0) {
                    payerPointsUsed = ptsAvailable - Math.abs(pointsToCover - ptsAvailable);
                    pointsToCover = 0L;
                    savedPayerPoints = savedPayerPoints + payerPointsUsed;
                    updatesSoFar.getIndicesToRemove().add(i);
                    updatesSoFar.setPointsSpent(savedPayerPoints);
                    pointsUsageByPayer.put(current.getPayer(), updatesSoFar);
                } else {
                    pointsToCover = pointsToCover - ptsAvailable;
                    savedPayerPoints = savedPayerPoints + ptsAvailable;
                    updatesSoFar.getIndicesToRemove().add(i);
                    updatesSoFar.setPointsSpent(savedPayerPoints);
                    pointsUsageByPayer.put(current.getPayer(), updatesSoFar);
                }
            }
            if (pointsToCover == 0) {
                break;
            }
            i++;
        }

        // Invalidate caches, then update by posting un-used transactions.
        trxCache.invalidate();
        balancesCache.invalidate();
        List<Transaction> unUsedTransactions = IntStream.range(0, allTransactions.size())
            .filter(index -> {
                Transaction trxToCheck = allTransactions.get(index);
                return !pointsUsageByPayer.get(trxToCheck.getPayer()).getIndicesToRemove().contains(index);
            }).mapToObj(allTransactions::get)
            .collect(Collectors.toList());
        unUsedTransactions.forEach(this::postTransaction);

        // Prepare return value based on usage.
        // Account for case in which only negative trxs were encountered for a payer.
        pointsUsageByPayer.forEach((key, value) -> {
            if (value.getPointsSpent() > 0) {
                Long spentPtsToNegative = value.getPointsSpent() * -1;
                PointsUsageSummary payerUsageSummary = new PointsUsageSummary(key, spentPtsToNegative);
                pointsUsageSummaries.add(payerUsageSummary);
            }
        });
        return pointsUsageSummaries;
    }

    private List<Transaction> getAllTransactionsSorted(Cache cache, Set<Object> allKeys) {
        List<Transaction> allTransactions = new ArrayList<>();
        allKeys.forEach(key -> {
            List<Transaction> transactions = (List<Transaction>) Optional.ofNullable(cache.get(key))
                    .map(Cache.ValueWrapper::get)
                    .orElse(new ArrayList());
            if (!transactions.isEmpty()) {
                allTransactions.addAll(transactions);
            }
        });
        if (!allTransactions.isEmpty()) {
            List<Transaction> sortedTransactions = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
            return sortedTransactions;
        }
        return allTransactions;
    }

    private Set<Object> getAllKeys(String cacheName){
        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        return nativeCache.asMap().keySet();
    }

}
