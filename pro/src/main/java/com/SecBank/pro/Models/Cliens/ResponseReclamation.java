package com.SecBank.pro.Models.Cliens;

<<<<<<< HEAD
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
<<<<<<< HEAD
=======
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
=======
import jakarta.persistence.*;
>>>>>>> 3d4f4bc (j'ai mes tache de spring 1)
>>>>>>> 47ea36a (les tache de spring 1 par Taoufik sont bien fait)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseReclamation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String contenu;
    private Date datResponse;
<<<<<<< HEAD
=======
    @ManyToOne
<<<<<<< HEAD
@JoinColumn(name = "reclamation_id")
private Reclamation reclamation;
=======
    @JoinColumn(name = "reclamation_id")
    private Reclamation reclamation;
>>>>>>> 3d4f4bc (j'ai mes tache de spring 1)
>>>>>>> 47ea36a (les tache de spring 1 par Taoufik sont bien fait)
}
