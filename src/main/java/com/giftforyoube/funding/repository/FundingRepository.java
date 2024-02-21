package com.giftforyoube.funding.repository;

import com.giftforyoube.funding.entity.Funding;
import com.giftforyoube.funding.entity.FundingStatus;
import com.giftforyoube.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    Page<Funding> findAllPageByStatus(FundingStatus status, Pageable pageable);
    @Query("SELECT f FROM Funding f")
    Page<Funding> findById(Pageable pageable);
    Page<Funding> findAll(Pageable pageable);
    Slice<Funding> findByStatus(FundingStatus fundingStatus, Pageable pageable);
    List<Funding> findByEndDateLessThanEqualAndStatus(LocalDate currentDate, FundingStatus fundingStatus);
  
    Funding findByUserAndStatus(User currentUser, FundingStatus fundingStatus);

    @Query("SELECT COUNT(f) FROM Funding f WHERE f.currentAmount >= f.targetAmount")
    Long countSuccessfulFundings();
}