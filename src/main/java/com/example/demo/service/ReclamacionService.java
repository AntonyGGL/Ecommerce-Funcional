package com.example.demo.service;

import com.example.demo.model.Reclamacion;
import com.example.demo.repository.ReclamacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReclamacionService {

    @Autowired
    private ReclamacionRepository reclamacionRepository;

    public Reclamacion registrar(Reclamacion reclamacion) {
        reclamacion.setEstado("PENDIENTE");
        return reclamacionRepository.save(reclamacion);
    }

    public List<Reclamacion> listarTodas() {
        return reclamacionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Reclamacion> listarPendientes() {
        return reclamacionRepository.findByEstadoOrderByCreatedAtDesc("PENDIENTE");
    }

    public List<Reclamacion> listarPorCorreo(String correo) {
        return reclamacionRepository.findByCorreoOrderByCreatedAtDesc(correo);
    }

    public Reclamacion marcarProcesada(Long id) {
        Reclamacion r = reclamacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamación no encontrada: " + id));
        r.setEstado("PROCESADA");
        return reclamacionRepository.save(r);
    }
}
