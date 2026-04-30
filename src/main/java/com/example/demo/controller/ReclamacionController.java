package com.example.demo.controller;

import com.example.demo.model.Reclamacion;
import com.example.demo.service.ReclamacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reclamaciones")
public class ReclamacionController {

    @Autowired
    private ReclamacionService reclamacionService;

    // Endpoint público — cualquier visitante puede registrar una reclamación
    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody Reclamacion reclamacion) {
        Reclamacion guardada = reclamacionService.registrar(reclamacion);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", guardada.getId(),
                "message", "Su reclamación ha sido registrada con el código #REC-" + guardada.getId() +
                        ". Nos comunicaremos contigo en un plazo máximo de 15 días hábiles."
        ));
    }

    // Solo ADMIN puede listar todas
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reclamacion>> listar() {
        return ResponseEntity.ok(reclamacionService.listarTodas());
    }

    // Usuario autenticado ve solo sus propias reclamaciones (por correo del JWT)
    @GetMapping("/mis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Reclamacion>> miasReclamaciones(Principal principal) {
        return ResponseEntity.ok(reclamacionService.listarPorCorreo(principal.getName()));
    }

    // Solo ADMIN puede marcar como procesada
    @PutMapping("/{id}/procesar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Reclamacion> procesar(@PathVariable Long id) {
        return ResponseEntity.ok(reclamacionService.marcarProcesada(id));
    }
}
