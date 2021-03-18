package com.danielhoward.fetchrewards.controllers;

import com.danielhoward.fetchrewards.svc.PointsService;
import com.danielhoward.fetchrewards.svcdata.PointsSpendEntry;
import com.danielhoward.fetchrewards.svcdata.PointsUsageSummary;
import com.danielhoward.fetchrewards.svcdata.Transaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.danielhoward.fetchrewards.controllers.Endpoints.BALANCES;
import static com.danielhoward.fetchrewards.controllers.Endpoints.POINTS;
import static com.danielhoward.fetchrewards.controllers.Endpoints.TRANSACTIONS;

@RequestMapping("/")
@RestController
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping(TRANSACTIONS)
    public void postTransaction(@RequestBody Transaction transaction) {
        pointsService.postTransaction(transaction);
    }

    @PostMapping(POINTS)
    public List<PointsUsageSummary> spendPoints(@RequestBody PointsSpendEntry spendEntry) {
        return pointsService.spendPoints(spendEntry);
    }

    @GetMapping(BALANCES)
    public Map<String, Long> getBalances() {
        return pointsService.getBalances();
    }


}