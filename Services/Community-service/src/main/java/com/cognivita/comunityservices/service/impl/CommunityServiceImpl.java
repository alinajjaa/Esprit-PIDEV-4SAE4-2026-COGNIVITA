package com.cognivita.comunityservices.service.impl;

import com.cognivita.comunityservices.dto.CommunityRequestDto;
import com.cognivita.comunityservices.dto.CommunityResponseDto;
import com.cognivita.comunityservices.entity.Community;
import com.cognivita.comunityservices.repository.CommunityRepository;
import com.cognivita.comunityservices.service.CommunityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;

    public CommunityServiceImpl(CommunityRepository communityRepository) {
        this.communityRepository = communityRepository;
    }

    @Override
    public CommunityResponseDto createCommunity(CommunityRequestDto request) {
        if (communityRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Community name already exists: " + request.getName());
        }

        Community community = new Community();
        community.setName(request.getName());
        community.setDescription(request.getDescription());

        return toResponse(communityRepository.save(community));
    }

    @Override
    public List<CommunityResponseDto> getAllCommunities() {
        return communityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CommunityResponseDto getCommunityById(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Community not found with id: " + id));
        return toResponse(community);
    }

    @Override
    public CommunityResponseDto updateCommunity(Long id, CommunityRequestDto request) {
        Community existing = communityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Community not found with id: " + id));

        boolean changed = false;

        if (request.getName() != null && !request.getName().isBlank()) {
            communityRepository.findByNameIgnoreCase(request.getName())
                    .filter(found -> !found.getId().equals(id))
                    .ifPresent(found -> {
                        throw new IllegalArgumentException("Community name already exists: " + request.getName());
                    });
            existing.setName(request.getName());
            changed = true;
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            existing.setDescription(request.getDescription());
            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        return toResponse(communityRepository.save(existing));
    }

    @Override
    public void deleteCommunity(Long id) {
        if (!communityRepository.existsById(id)) {
            throw new IllegalArgumentException("Community not found with id: " + id);
        }
        communityRepository.deleteById(id);
    }

    private CommunityResponseDto toResponse(Community community) {
        return new CommunityResponseDto(
                community.getId(),
                community.getName(),
                community.getDescription()
        );
    }
}
