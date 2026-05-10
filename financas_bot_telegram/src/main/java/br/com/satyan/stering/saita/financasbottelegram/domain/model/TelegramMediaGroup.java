package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMediaGroup {

  private String fileIdPedido;
  private String fileIdComprovante;
  private String categoria;
  private String origem;
  private String urlPedido;
  private String urlComprovante;

}
