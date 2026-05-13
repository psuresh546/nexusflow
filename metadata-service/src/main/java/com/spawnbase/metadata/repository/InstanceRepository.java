package com.spawnbase.metadata.repository;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.entity.Instance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {


    List<Instance> findByOwnerId(String ownerId);

    List<Instance> findByState(InstanceState state);

    Optional<Instance> findByContainerId(String containerId);

    @Query("SELECT i FROM Instance i WHERE " + "i.ownerId = :ownerId AND " + "i.state <> 'DELETED'")
    List<Instance> findActiveByOwnerId(@Param("ownerId") String ownerId);


    Page<Instance> findByState(InstanceState state, Pageable pageable);

    Page<Instance> findByDbType(DatabaseType dbType, Pageable pageable);

    Page<Instance> findByStateAndDbType(InstanceState state, DatabaseType dbType, Pageable pageable);
}