// com/window/service/CaseService.java
package com.window.service;

import com.window.entity.Case;
import java.util.List;

public interface CaseService {

    List<Case> listAll();

    List<Case> listVisible();

    Case getById(Integer id);

    void save(Case c);

    void update(Case c);

    void removeById(Integer id);

}
