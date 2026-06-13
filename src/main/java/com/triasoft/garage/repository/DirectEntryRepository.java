package com.triasoft.garage.repository;

import com.triasoft.garage.entity.DirectEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectEntryRepository extends JpaRepository<DirectEntry, Long> {

    Page<DirectEntry> findAllByOrderByEntryDateDescCreatedAtDesc(Pageable pageable);

}
