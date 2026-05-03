package com.kworkerharmony.backend.reference;

import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.reference.educationvenue.EducationVenueCatalog;
import com.kworkerharmony.backend.reference.educationvenue.EducationVenueDefinition;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final EducationVenueCatalog educationVenueCatalog;

    @GetMapping("/education-venues")
    public ApiResponse<List<EducationVenueDefinition>> getEducationVenues(
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<EducationVenueDefinition> venues = region == null || region.isBlank()
                ? educationVenueCatalog.getAll().stream().limit(safeLimit).toList()
                : educationVenueCatalog.findByRegion(region, safeLimit);
        return ApiResponse.success(venues);
    }
}
