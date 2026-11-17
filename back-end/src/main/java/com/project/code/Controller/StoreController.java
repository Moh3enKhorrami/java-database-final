package com.project.code.Controller;

import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.StoreRepository;
import com.project.code.Service.OrderService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Map<String, String> addStore(@RequestBody Store store) {
        Store saved = storeRepository.save(store);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Store added successfully with id: " + saved.getId());
        return response;
    }

    /** Returns 200 with true when the store exists, 404 with false when it does not. */
    @GetMapping("validate/{storeId}")
    public ResponseEntity<Boolean> validateStore(@PathVariable Long storeId) {
        boolean exists = storeRepository.findByid(storeId) != null;
        return ResponseEntity.status(exists ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(exists);
    }

    /** Returns 200 on success; 400 with an "error" message when the order cannot be placed. */
    @PostMapping("/placeOrder")
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody PlaceOrderRequestDTO placeOrderRequest) {
        Map<String, String> response = new HashMap<>();
        try {
            orderService.saveOrder(placeOrderRequest);
            response.put("message", "Order placed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
