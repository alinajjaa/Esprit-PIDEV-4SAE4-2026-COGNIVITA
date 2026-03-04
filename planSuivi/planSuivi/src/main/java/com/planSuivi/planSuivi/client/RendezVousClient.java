package com.planSuivi.planSuivi.client;

import com.planSuivi.planSuivi.dto.RendezVousSimpleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-rdv", url = "${rdv.service.url}")
public interface RendezVousClient {

    @GetMapping("/api/rendezvous/{id}/simple")
    RendezVousSimpleDto getSimpleById(@PathVariable("id") Long id);
}