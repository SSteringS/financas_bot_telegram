package br.com.satyan.stering.saita.financasbottelegram.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pagamento")
public class Pagamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;
  private String urlPedido;
  private String urlComprovante;
  private String categoria;
  private String origem;
  private LocalDateTime dataHora;

}