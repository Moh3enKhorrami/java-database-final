package com.project.code.Controller;

import com.project.code.Model.Customer;
import com.project.code.Model.Review;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.ReviewRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/{storeId}/{productId}")
    public Map<String, Object> getReviews(@PathVariable Long storeId, @PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByStoreIdAndProductId(storeId, productId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Review review : reviews) {
            Customer customer = customerRepository.findByid(review.getCustomerId());
            Map<String, Object> item = new HashMap<>();
            item.put("comment", review.getComment());
            item.put("rating", review.getRating());
            item.put("customerName", customer != null ? customer.getName() : "Unknown");
            result.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", result);
        return response;
    }
}
