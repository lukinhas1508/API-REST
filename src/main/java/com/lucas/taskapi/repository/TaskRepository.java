package com.lucas.taskapi.repository;

import com.lucas.taskapi.entity.Task;
import com.lucas.taskapi.entity.TaskPriority;
import com.lucas.taskapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.owner.id = :ownerId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
            """)
    Page<Task> search(@Param("ownerId") Long ownerId,
                      @Param("status") TaskStatus status,
                      @Param("priority") TaskPriority priority,
                      Pageable pageable);
}
