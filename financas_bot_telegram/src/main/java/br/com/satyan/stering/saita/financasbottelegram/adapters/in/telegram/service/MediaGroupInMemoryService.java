package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class MediaGroupInMemoryService {

  private final ConcurrentHashMap<String, TelegramMediaGroup> mediaGroupMap = new ConcurrentHashMap<>();

  public TelegramMediaGroup addAndGetGroup(String mediaGroupId, String fileId, String categoria,
      String origem) {

    TelegramMediaGroup group = mediaGroupMap.computeIfAbsent(mediaGroupId,
        k -> new TelegramMediaGroup());

    setCategoriaIfNotNull(group, categoria);
    setOrigemIfNotNull(group, origem);
    setUrlPedidoOrComprovante(group, fileId);

    return group;
  }

  private void setCategoriaIfNotNull(TelegramMediaGroup group, String categoria) {
    if (categoria != null) {
      group.setCategoria(categoria);
    }
  }

  private void setOrigemIfNotNull(TelegramMediaGroup group, String origem) {
    if (origem != null) {
      group.setOrigem(origem);
    }
  }

  private void setUrlPedidoOrComprovante(TelegramMediaGroup group, String fileId) {
    if (group.getFileIdPedido() == null) {
      group.setFileIdPedido(fileId);
    } else if (group.getUrlComprovante() == null) {
      group.setFileIdComprovante(fileId);
    }
  }

  public void removeGroup(String mediaGroupId) {
    mediaGroupMap.remove(mediaGroupId);
  }
}