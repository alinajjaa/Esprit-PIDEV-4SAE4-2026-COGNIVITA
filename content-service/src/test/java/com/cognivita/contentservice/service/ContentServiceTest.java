package com.cognivita.contentservice.service;

import com.cognivita.contentservice.dto.CommunityRequestDto;
import com.cognivita.contentservice.dto.CommunityResponseDto;
import com.cognivita.contentservice.entity.Community;
import com.cognivita.contentservice.repository.CommunityRepository;
import com.cognivita.contentservice.service.impl.CommunityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private CommunityRepository communityRepository;

    @InjectMocks
    private CommunityServiceImpl contentService;

    @Test
    void createCommunity_savesAndReturnsCommunity() {
        CommunityRequestDto request = new CommunityRequestDto();
        request.setName("Care Circle");
        request.setDescription("Support community for caregivers");

        Community savedCommunity = new Community();
        savedCommunity.setId(1L);
        savedCommunity.setName("Care Circle");
        savedCommunity.setDescription("Support community for caregivers");

        when(communityRepository.existsByNameIgnoreCase("Care Circle")).thenReturn(false);
        when(communityRepository.save(any(Community.class))).thenReturn(savedCommunity);

        CommunityResponseDto result = contentService.createCommunity(request);

        ArgumentCaptor<Community> communityCaptor = ArgumentCaptor.forClass(Community.class);
        verify(communityRepository, times(1)).save(communityCaptor.capture());
        verify(communityRepository, times(1)).existsByNameIgnoreCase(eq("Care Circle"));

        Community communityToPersist = communityCaptor.getValue();
        assertEquals("Care Circle", communityToPersist.getName());
        assertEquals("Support community for caregivers", communityToPersist.getDescription());

        assertNotNull(result);
        assertEquals("Care Circle", result.getName());
        assertEquals("Support community for caregivers", result.getDescription());
    }

    @Test
    void createCommunity_throwsExceptionWhenNameIsNullOrBlank() {
        CommunityRequestDto nullNameRequest = new CommunityRequestDto();
        nullNameRequest.setName(null);
        nullNameRequest.setDescription("Description");

        CommunityRequestDto blankNameRequest = new CommunityRequestDto();
        blankNameRequest.setName("   ");
        blankNameRequest.setDescription("Description");

        IllegalArgumentException nullNameException = assertThrows(
                IllegalArgumentException.class,
                () -> contentService.createCommunity(nullNameRequest)
        );
        IllegalArgumentException blankNameException = assertThrows(
                IllegalArgumentException.class,
                () -> contentService.createCommunity(blankNameRequest)
        );

        assertEquals("Community name is required", nullNameException.getMessage());
        assertEquals("Community name is required", blankNameException.getMessage());

        verifyNoInteractions(communityRepository);
        verify(communityRepository, never()).save(any(Community.class));
    }
}
