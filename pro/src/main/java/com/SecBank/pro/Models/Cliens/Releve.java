package com.SecBank.pro.Models.Cliens;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Releve {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String periode;
    private LocalDateTime dateGeneration;
    private String fichier;

    @ManyToOne
    private CompteBancaire compte;
}
