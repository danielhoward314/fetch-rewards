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
        Cache trxCache = cacheManager.getCache(TRANSACTIONS_CACHE);
        Set<Object> allTrxKeys = getAllKeys(TRANSACTIONS_CACHE);
        Cache balancesCache = cacheManager.getCache(BALANCES_CACHE);
        // Merges all payer transactions into sorted list to enforce earliest timestamp rule.
        List<Transaction> allTransactions = getAllTransactionsSorted(trxCache, allTrxKeys);
        Long pointsToCover = spendEntry.getPoints();
        // Intermediate data structure that will be used to prepare return value.
        Map<String, UpdateLedger> pointsUsageByPayer = new HashMap<>();
        List<Transaction> diffTransactions = new ArrayList<>();
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
                pointsToCover = pointsToCover + Math.abs(current.getPoints());
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
                    // Since we will remove the old transaction below, but did not use all of its points
                    // add a new transaction to account for the difference, using original timestamp.
                    Long leftOverPts = current.getPoints() - payerPointsUsed;
                    Transaction diffTrx = new Transaction(current.getPayer(), leftOverPts, current.getTimestamp());
                    diffTransactions.add(diffTrx);
                } else {
                    pointsToCover = pointsToCover - ptsAvailable;
                    savedPayerPoints = savedPayerPoints + ptsAvailable;
                }
                updatesSoFar.getIndicesToRemove().add(i);
                updatesSoFar.setPointsSpent(savedPayerPoints);
                pointsUsageByPayer.put(current.getPayer(), updatesSoFar);
            }
            if (pointsToCover == 0) {
                balancesCache.invalidate();
                pointsUsageByPayer.forEach((payer, ledger) -> {
                    // Account for case when all transactions for a payer are used.
                    // Update balance cache with 0, rather than rely on posting unused transactions for balance update.
                    Integer idxToRmv = ledger.getIndicesToRemove().size();
                    Integer trxCountForPayer = Math.toIntExact(allTransactions.stream()
                        .filter(transaction -> transaction.getPayer().equals(payer))
                        .count());
                    if (idxToRmv == trxCountForPayer) {
                        balancesCache.put(payer, 0L);
                    }
                });
                break;
            }
            i++;
        }

        // Invalidate transaction cache, then update by posting unused transactions.
        trxCache.invalidate();
        List<Transaction> unUsedTransactions = IntStream.range(0, allTransactions.size())
            .filter(index -> {
                Transaction trxToCheck = allTransactions.get(index);
                return !pointsUsageByPayer.get(trxToCheck.getPayer()).getIndicesToRemove().contains(index);
            }).mapToObj(allTransactions::get)
            .collect(Collectors.toList());
        unUsedTransactions.addAll(diffTransactions);
        unUsedTransactions.forEach(this::postTransaction);

        // Prepare return value based on usage.
        List<PointsUsageSummary> pointsUsageSummaries = new ArrayList<>();
        pointsUsageByPayer.forEach((key, value) -> {
            if (value.getPointsSpent() > 0) {
                Long spentPtsToNegative = value.getPointsSpent() * -1;
                PointsUsageSummary payerUsageSummary = new PointsUsageSummary(key, spentPtsToNegative);
                pointsUsageSummaries.add(payerUsageSummary);
            }
        });
        return pointsUsageSummaries;
    }

    public Map<String, Long> getBalances() {
        Cache balancesCache = cacheManager.getCache(BALANCES_CACHE);
        Set<Object> allBalancesKeys = getAllKeys(BALANCES_CACHE);
        Map<String, Long> allBalances = new HashMap<>();
        allBalancesKeys.forEach(key -> {
            String payerName = (String) key;
            Long points = (Long) Optional.ofNullable(balancesCache.get(key))
                .map(Cache.ValueWrapper::get)
                .orElse(0L);
            allBalances.put(payerName, points);
        });
        return allBalances;
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
