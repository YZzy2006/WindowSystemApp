// com/window/service/ProductService.java
package com.window.service;

import com.window.entity.Product;
import java.util.List;

public interface ProductService {

    List<Product> listByCategory(Integer categoryId);

    List<Product> listAll();

    Product getById(Integer id);

    void save(Product product);

    void updateById(Product product);

    void removeById(Integer id);

}
