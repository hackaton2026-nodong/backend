package com.kworkerharmony.backend.user;

import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.reference.country.CountryCatalog;
import com.kworkerharmony.backend.reference.language.LanguageCatalog;
import com.kworkerharmony.backend.user.dto.request.UpdateMyProfileRequest;
import com.kworkerharmony.backend.user.dto.response.MyProfileResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CountryCatalog countryCatalog;
    private final LanguageCatalog languageCatalog;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(UserPrincipal userPrincipal) {
        return MyProfileResponse.from(getUser(userPrincipal));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(UpdateMyProfileRequest request, UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateCountryCode(request.countryCode());
        validateLanguageCode(request.languageCode());
        if (request.visaExpiresAt() != null && request.visaExpiresAt().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Visa expiry date cannot be in the past");
        }

        user.updateProfile(
                request.name(),
                request.phoneNumber(),
                countryCatalog.normalize(request.countryCode()),
                languageCatalog.normalize(request.languageCode()),
                user.getUserType() == UserType.WORKER ? request.visaExpiresAt() : null
        );
        return MyProfileResponse.from(user);
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void validateCountryCode(String countryCode) {
        if (!countryCatalog.exists(countryCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Country not found");
        }
    }

    private void validateLanguageCode(String languageCode) {
        if (!languageCatalog.exists(languageCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Language not found");
        }
    }
}
