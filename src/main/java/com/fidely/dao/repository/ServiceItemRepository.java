package com.fidely.dao.repository;

import com.fidely.domain.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByBusinessId(Long businessId);
    List<ServiceItem> findByIdInAndBusinessId(List<Long> ids, Long businessId);
}
