package com.project.code.Service;

import com.project.code.Model.Customer;
import com.project.code.Model.Inventory;
import com.project.code.Model.OrderDetails;
import com.project.code.Model.OrderItem;
import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.Product;
import com.project.code.Model.PurchaseProductDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.OrderDetailsRepository;
import com.project.code.Repo.OrderItemRepository;
import com.project.code.Repo.ProductRepository;
import com.project.code.Repo.StoreRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /** All or nothing: any failure (unknown store or product, not enough stock) rolls back the whole order. */
    @Transactional
    public void saveOrder(PlaceOrderRequestDTO placeOrderRequest) {
        // 1. Retrieve or create the customer
        Customer customer = customerRepository.findByEmail(placeOrderRequest.getCustomerEmail());
        if (customer == null) {
            customer = new Customer();
            customer.setName(placeOrderRequest.getCustomerName());
            customer.setEmail(placeOrderRequest.getCustomerEmail());
            customer.setPhone(placeOrderRequest.getCustomerPhone());
            customer = customerRepository.save(customer);
        }

        // 2. Retrieve the store
        Store store = storeRepository.findById(placeOrderRequest.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        // 3. Create the order
        OrderDetails order = new OrderDetails(customer, store, placeOrderRequest.getTotalPrice(), LocalDateTime.now());
        order = orderDetailsRepository.save(order);

        // 4-5. Update stock and create the order items
        for (PurchaseProductDTO purchase : placeOrderRequest.getPurchaseProduct()) {
            Product product = productRepository.findByid(purchase.getId());
            if (product == null) {
                throw new RuntimeException("Product not found: " + purchase.getId());
            }

            Inventory inventory = inventoryRepository.findByProductIdandStoreId(purchase.getId(), store.getId());
            if (inventory == null) {
                throw new RuntimeException("Product " + product.getName() + " is not available in this store");
            }
            if (inventory.getStockLevel() < purchase.getQuantity()) {
                throw new RuntimeException("Not enough stock for " + product.getName());
            }
            inventory.setStockLevel(inventory.getStockLevel() - purchase.getQuantity());
            inventoryRepository.save(inventory);

            OrderItem item = new OrderItem(order, product, purchase.getQuantity(), purchase.getPrice());
            orderItemRepository.save(item);
        }
    }
}
