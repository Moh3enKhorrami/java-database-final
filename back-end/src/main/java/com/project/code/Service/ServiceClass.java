package com.project.code.Service;

import com.project.code.Model.Inventory;
import com.project.code.Model.Product;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceClass {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public ServiceClass(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    /** Returns false if an inventory record already exists for the product and store, otherwise true. */
    public boolean validateInventory(Inventory inventory) {
        return inventoryRepository.findByProductIdandStoreId(
                inventory.getProduct().getId(), inventory.getStore().getId()) == null;
    }

    /** Returns false if a product with the same name already exists, otherwise true. */
    public boolean validateProduct(Product product) {
        return productRepository.findByName(product.getName()) == null;
    }

    /** Returns false if the product does not exist with the given id, otherwise true. */
    public boolean ValidateProductId(long id) {
        return productRepository.findByid(id) != null;
    }

    /** Returns the inventory record for the product and store of the given inventory. */
    public Inventory getInventoryId(Inventory inventory) {
        return inventoryRepository.findByProductIdandStoreId(
                inventory.getProduct().getId(), inventory.getStore().getId());
    }

    /**
     * Deletes the product together with its inventory rows in one transaction:
     * if either delete fails, both are rolled back.
     */
    @Transactional
    public void deleteProductWithInventory(Long productId) {
        inventoryRepository.deleteByProductId(productId);
        productRepository.deleteById(productId);
        productRepository.flush();
    }
}
