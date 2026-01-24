package com.taskmanager.domain.repository;


import com.taskmanager.domain.model.Tasks;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Tasks, Long>{

	Optional<Tasks> findByTitle(String title);

    Tasks save(Tasks tasks);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
	 Optional<Tasks> findById(Long id);
}