package com.cognivita.contentservice.service;

import com.cognivita.contentservice.dto.CommunityRequestDto;
import com.cognivita.contentservice.dto.CommunityResponseDto;

import java.util.List;

public interface CommunityService {

    CommunityResponseDto createCommunity(CommunityRequestDto request);

    List<CommunityResponseDto> getAllCommunities();

    CommunityResponseDto getCommunityById(Long id);

    CommunityResponseDto updateCommunity(Long id, CommunityRequestDto request);

    void deleteCommunity(Long id);
}
