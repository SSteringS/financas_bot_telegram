package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MediaGroupInMemoryService {

    private final ConcurrentHashMap<String, List<String>> mediaGroupMap = new ConcurrentHashMap<>();

    public List<String> addAndGetGroup(String mediaGroupId, String fileId) {
        List<String> group = mediaGroupMap.computeIfAbsent(mediaGroupId, k -> new ArrayList<>());
        group.add(fileId);
        return group;
    }

    public void removeGroup(String mediaGroupId) {
        mediaGroupMap.remove(mediaGroupId);
    }
}