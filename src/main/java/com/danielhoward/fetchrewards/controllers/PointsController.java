package com.danielhoward.fetchrewards.controllers;

import com.danielhoward.fetchrewards.svc.PointsService;
import com.danielhoward.fetchrewards.svcdata.Transaction;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.danielhoward.fetchrewards.controllers.Endpoints.TRANSACTIONS;

@RequestMapping("/")
@RestController
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping(TRANSACTIONS)
    public List<Transaction> postTransaction(@RequestBody Transaction transaction) {
        return pointsService.postTransaction(transaction);
    }

}