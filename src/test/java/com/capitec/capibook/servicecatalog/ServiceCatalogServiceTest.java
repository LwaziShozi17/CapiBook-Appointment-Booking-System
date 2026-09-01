package com.capitec.capibook.servicecatalog;

import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.dto.BankingServiceResponse;
import com.capitec.capibook.servicecatalog.dto.CreateServiceRequest;
import com.capitec.capibook.servicecatalog.dto.UpdateServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private BankingServiceRepository repository;

    @InjectMocks
    private ServiceCatalogService serviceCatalogService;

    private BankingService testService;

    @BeforeEach
    void setUp() {
        testService = new BankingService();
        testService.setName("Card Collection");
        testService.setDescription("Collect a card from the branch.");
        testService.setDurationMinutes(15);
        testService.setActive(true);
    }

    @Test
    void create_withNewName_returnsResponse() {
        when(repository.existsByName("Card Collection")).thenReturn(false);
        when(repository.save(any(BankingService.class))).thenReturn(testService);

        BankingServiceResponse response = serviceCatalogService.create(
                new CreateServiceRequest("Card Collection", "Collect a card.", 15));

        assertThat(response.name()).isEqualTo("Card Collection");
        assertThat(response.durationMinutes()).isEqualTo(15);
        verify(repository).save(any(BankingService.class));
    }

    @Test
    void create_withDuplicateName_throwsDuplicateResourceException() {
        when(repository.existsByName("Card Collection")).thenReturn(true);

        assertThatThrownBy(() -> serviceCatalogService.create(
                new CreateServiceRequest("Card Collection", "Desc", 15)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Card Collection");

        verify(repository, never()).save(any());
    }

    @Test
    void listActive_returnsOnlyActiveServices() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of(testService));

        List<BankingServiceResponse> results = serviceCatalogService.listActive();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Card Collection");
    }

    @Test
    void getById_withExistingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testService));

        BankingServiceResponse response = serviceCatalogService.getById(id);

        assertThat(response.name()).isEqualTo("Card Collection");
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withValidRequest_updatesFields() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testService));
        when(repository.existsByNameAndIdNot("Card Collection Updated", id)).thenReturn(false);
        when(repository.save(any(BankingService.class))).thenReturn(testService);

        serviceCatalogService.update(id, new UpdateServiceRequest("Card Collection Updated", "New desc", 20));

        assertThat(testService.getName()).isEqualTo("Card Collection Updated");
        assertThat(testService.getDurationMinutes()).isEqualTo(20);
        verify(repository).save(testService);
    }

    @Test
    void update_withDuplicateName_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testService));
        when(repository.existsByNameAndIdNot("Existing Service", id)).thenReturn(true);

        assertThatThrownBy(() -> serviceCatalogService.update(id,
                new UpdateServiceRequest("Existing Service", "Desc", 15)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_setsActiveFalse() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testService));
        when(repository.save(any(BankingService.class))).thenReturn(testService);

        serviceCatalogService.deactivate(id);

        assertThat(testService.isActive()).isFalse();
        verify(repository).save(testService);
    }
}
