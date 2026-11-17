package com.project.code.Controller;

import com.project.code.Model.CombinedRequest;
import com.project.code.Model.Inventory;
import com.project.code.Model.Product;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.ProductRepository;
import com.project.code.Service.ServiceClass;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ServiceClass serviceClass;

    @PutMapping
    public Map<String, String> updateInventory(@RequestBody CombinedRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            Product product = request.getProduct();
            Inventory inventory = request.getInventory();

            if (!serviceClass.ValidateProductId(product.getId())) {
                response.put("message", "Product id does not exist");
                return response;
            }
            // Back references are not read from JSON, so link the inventory to the product here
            inventory.setProduct(product);

            Inventory existing = serviceClass.getInventoryId(inventory);
            if (existing == null) {
                response.put("message", "No data available");
                return response;
            }
            productRepository.save(product);
            existing.setStockLevel(inventory.getStockLevel());
            inventoryRepository.save(existing);
            response.put("message", "Successfully updated product");
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Error: " + e.getMessage());
        } catch (Exception e) {
            response.put("message", "Error: " + e.getMessage());
        }
        return response;
    }

    @PostMapping
    public Map<String, String> saveInventory(@RequestBody Inventory inventory) {
        Map<String, String> response = new HashMap<>();
        try {
            if (!serviceClass.validateInventory(inventory)) {
                response.put("message", "Data already present");
                return response;
            }
            inventoryRepository.save(inventory);
            response.put("message", "Data saved successfully");
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Error: " + e.getMessage());
        } catch (Exception e) {
            response.put("message", "Error: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/{storeid}")
    public Map<String, Object> getAllProducts(@PathVariable Long storeid) {
        Map<String, Object> response = new HashMap<>();
        response.put("products", withStoreStock(productRepository.findProductsByStoreId(storeid), storeid));
        return response;
    }

    /**
     * Filters one store's inventory by category and/or name; the literal "null" means "no filter".
     * Both "null" returns the whole store. A missing or invalid store id gives 400 with an empty list.
     */
    @GetMapping("filter/{category}/{name}/{storeid}")
    public ResponseEntity<Map<String, Object>> getProductName(@PathVariable String category,
            @PathVariable String name, @PathVariable Long storeid) {
        Map<String, Object> response = new HashMap<>();
        if (storeid == null || storeid <= 0) {
            response.put("message", "A valid store id is required");
            response.put("product", List.of());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        boolean noCategory = category == null || "null".equals(category);
        boolean noName = name == null || "null".equals(name);
        List<Product> products;
        if (noCategory && noName) {
            products = productRepository.findProductsByStoreId(storeid);
        } else if (noCategory) {
            products = productRepository.findByNameLike(storeid, name);
        } else if (noName) {
            products = productRepository.findByCategoryAndStoreId(storeid, category);
        } else {
            products = productRepository.findByNameAndCategory(storeid, name, category);
        }
        response.put("product", withStoreStock(products, storeid));
        return ResponseEntity.ok(response);
    }

    @GetMapping("search/{name}/{storeId}")
    public Map<String, Object> searchProduct(@PathVariable String name, @PathVariable Long storeId) {
        Map<String, Object> response = new HashMap<>();
        response.put("product", withStoreStock(productRepository.findByNameLike(storeId, name), storeId));
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> removeProduct(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        if (!serviceClass.ValidateProductId(id)) {
            response.put("message", "Product not present in database");
            return response;
        }
        inventoryRepository.deleteByProductId(id);
        response.put("message", "Deleted product successfully with id: " + id);
        return response;
    }

    /**
     * Checks that the store holds at least the requested quantity of the product.
     * 200 true: enough stock; 409 false: not enough; 404 false: product not stocked in this store;
     * 400 false: quantity is not positive.
     */
    @GetMapping("validate/{quantity}/{storeId}/{productId}")
    public ResponseEntity<Boolean> validateQuantity(@PathVariable Integer quantity, @PathVariable Long storeId,
            @PathVariable Long productId) {
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
        Inventory inventory = inventoryRepository.findByProductIdandStoreId(productId, storeId);
        if (inventory == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
        if (inventory.getStockLevel() < quantity) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
        }
        return ResponseEntity.ok(true);
    }

    /**
     * The front end reads product.inventory[0].stockLevel, so keep only this store's
     * stock row on each returned product (the objects are detached, nothing is saved).
     */
    private List<Product> withStoreStock(List<Product> products, Long storeId) {
        for (Product product : products) {
            Inventory stock = inventoryRepository.findByProductIdandStoreId(product.getId(), storeId);
            product.setInventory(stock == null ? List.of() : List.of(stock));
        }
        return products;
    }
}
