package com.capitec.capibook.servicecatalog;

import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.dto.BankingServiceResponse;
import com.capitec.capibook.servicecatalog.dto.CreateServiceRequest;
import com.capitec.capibook.servicecatalog.dto.UpdateServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ServiceCatalogService {

    private final BankingServiceRepository repository;

    public ServiceCatalogService(BankingServiceRepository repository) {
        this.repository = repository;
    }

    public BankingServiceResponse create(CreateServiceRequest request) {
        if (repository.existsByName(request.name())) {
            throw new DuplicateResourceException("Service name already exists: " + request.name());
        }
        BankingService service = new BankingService();
        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        return BankingServiceResponse.from(repository.save(service));
    }

    @Transactional(readOnly = true)
    public List<BankingServiceResponse> listActive() {
        return repository.findAllByActiveTrue().stream()
                .map(BankingServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BankingServiceResponse getById(UUID id) {
        return BankingServiceResponse.from(findById(id));
    }

    public BankingServiceResponse update(UUID id, UpdateServiceRequest request) {
        BankingService service = findById(id);
        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Service name already exists: " + request.name());
        }
        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        return BankingServiceResponse.from(repository.save(service));
    }

    public void deactivate(UUID id) {
        BankingService service = findById(id);
        service.setActive(false);
        repository.save(service);
    }

    private BankingService findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }
}
