// com/window/service/CategoryService.java
package com.window.service;

import com.window.entity.Category;
import java.util.List;

public interface CategoryService {

    List<Category> list();

    void save(Category category);

    void updateById(Category category);

    void removeById(Integer id);

}
