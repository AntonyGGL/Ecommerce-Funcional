package com.example.demo.repository;

import com.example.demo.model.Reclamacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReclamacionRepository extends JpaRepository<Reclamacion, Long> {
    List<Reclamacion> findByEstadoOrderByCreatedAtDesc(String estado);
    List<Reclamacion> findAllByOrderByCreatedAtDesc();
    List<Reclamacion> findByCorreoOrderByCreatedAtDesc(String correo);
}
