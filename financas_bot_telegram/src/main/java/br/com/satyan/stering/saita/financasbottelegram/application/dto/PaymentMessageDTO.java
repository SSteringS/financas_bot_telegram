package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessageDTO {
    private List<String> imageIds;
    private PaymentCategory categoria;
    private String origem;
}
