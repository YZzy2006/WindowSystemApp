// com/window/service/EnquiryService.java
package com.window.service;

import com.window.entity.Enquiry;
import java.util.List;

public interface EnquiryService {

    void save(Enquiry enquiry);

    List<Enquiry> listAll();

    void markRead(Integer id);

    void markMeasured(Integer id, Integer measured);

    void markStarred(Integer id, Integer starred);

    void markCompleted(Integer id, Integer completed);

    void markAllRead();

    void updateRemark(Integer id, String remark);

    void removeById(Integer id);

}
