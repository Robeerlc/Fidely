package com.fidely.dao.repository;

import com.fidely.domain.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    List<Promotion> findByBusinessIdAndActiveTrueOrderByCreatedAtDesc(Long businessId);

    Optional<Promotion> findByIdAndBusinessId(Long id, Long businessId);
}
