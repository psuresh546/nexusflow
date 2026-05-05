package com.spawnbase.metadata.repository;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.entity.Instance;
import com.spawnbase.provisioning.model.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface InstanceRepository
        extends JpaRepository<Instance, UUID> {


    List<Instance> findByOwnerId(String ownerId);

    List<Instance> findByState(InstanceState state);

    List<Instance> findByDbType(DatabaseType dbType);

    List<Instance> findByOwnerIdAndState(
            String ownerId,
            InstanceState state
    );

    Optional<Instance> findByContainerId(String containerId);

    @Query("SELECT i.state, COUNT(i) FROM Instance i GROUP BY i.state")
    List<Object[]> countByState();

    @Query("SELECT i FROM Instance i WHERE i.ownerId = :ownerId " +
            "AND i.state != com.spawnbase.common.model.InstanceState.DELETED")
    List<Instance> findActiveByOwnerId(@Param("ownerId") String ownerId);
}