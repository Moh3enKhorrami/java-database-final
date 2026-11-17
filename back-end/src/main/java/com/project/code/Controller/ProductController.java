package com.project.code.Controller;

import com.project.code.Model.Product;
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
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ServiceClass serviceClass;

    @PostMapping
    public Map<String, String> addProduct(@RequestBody Product product) {
        Map<String, String> response = new HashMap<>();
        if (!serviceClass.validateProduct(product)) {
            response.put("message", "Product already present in database");
            return response;
        }
        try {
            productRepository.save(product);
            response.put("message", "Product added successfully");
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Error: SKU should be unique");
        }
        return response;
    }

    /** Returns 200 with the product, or 404 with a message when no product has this id. */
    @GetMapping("/product/{id}")
    public ResponseEntity<Map<String, Object>> getProductbyId(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Product product = productRepository.findByid(id);
        if (product == null) {
            response.put("message", "Product not found with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        response.put("products", product);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public Map<String, String> updateProduct(@RequestBody Product product) {
        Map<String, String> response = new HashMap<>();
        try {
            productRepository.save(product);
            response.put("message", "Product updated successfully");
        } catch (Exception e) {
            response.put("message", "Error updating product: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/category/{name}/{category}")
    public Map<String, Object> filterbyCategoryProduct(@PathVariable String name, @PathVariable String category) {
        List<Product> products;
        boolean noName = "null".equals(name);
        boolean noCategory = "null".equals(category);
        if (noName && noCategory) {
            products = productRepository.findAll();
        } else if (noName) {
            products = productRepository.findByCategory(category);
        } else if (noCategory) {
            products = productRepository.findProductBySubName(name);
        } else {
            products = productRepository.findProductBySubNameAndCategory(name, category);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        return response;
    }

    @GetMapping
    public Map<String, Object> listProduct() {
        Map<String, Object> response = new HashMap<>();
        response.put("products", productRepository.findAll());
        return response;
    }

    @GetMapping("filter/{category}/{storeid}")
    public Map<String, Object> getProductbyCategoryAndStoreId(@PathVariable String category, @PathVariable Long storeid) {
        Map<String, Object> response = new HashMap<>();
        response.put("product", productRepository.findProductByCategory(category, storeid));
        return response;
    }

    /** Deletes the product and its inventory in one transaction (see ServiceClass). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        if (!serviceClass.ValidateProductId(id)) {
            response.put("message", "Product not present in database");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        try {
            serviceClass.deleteProductWithInventory(id);
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Product cannot be deleted because it is used in orders");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.put("message", "Error deleting product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/searchProduct/{name}")
    public Map<String, Object> searchProduct(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("products", productRepository.findProductBySubName(name));
        return response;
    }
}
