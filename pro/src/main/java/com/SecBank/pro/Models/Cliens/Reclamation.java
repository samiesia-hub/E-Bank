package com.SecBank.pro.Models.Cliens;

import com.SecBank.pro.Models.Énums.StatusReclamation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String Objet;
    private String desciprtion;
    private Date dateCreation;
    @Enumerated(value = EnumType.STRING)
    private StatusReclamation status;
}
