package com.example.deployer.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationProviderTest {

    @Mock
    private TokenRepo repo;

    @InjectMocks
    private TokenAuthenticationProvider provider;

    // -------------------------------------------------------------------------
    // Valid tokens
    // -------------------------------------------------------------------------

    @Test
    void authenticate_tokenValidForToday_returnsAuthenticatedToken() {
        OneTimeToken ott = new OneTimeToken(1L, "good-token", LocalDate.now());
        when(repo.findByToken("good-token")).thenReturn(Optional.of(ott));

        Authentication result = provider.authenticate(auth("good-token"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("good-token");
    }

    @Test
    void authenticate_firstUse_setsValidUntilAndReturnsAuthenticated() {
        // validUntil == null means the token has never been used
        OneTimeToken ott = new OneTimeToken(1L, "new-token", null);
        when(repo.findByToken("new-token")).thenReturn(Optional.of(ott));

        Authentication result = provider.authenticate(auth("new-token"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(ott.getValidUntil()).isEqualTo(LocalDate.now());
        verify(repo).save(ott);
    }

    // -------------------------------------------------------------------------
    // Invalid / expired tokens
    // -------------------------------------------------------------------------

    @Test
    void authenticate_unknownToken_throwsBadCredentialsException() {
        when(repo.findByToken("unknown")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                provider.authenticate(auth("unknown")));
    }

    @Test
    void authenticate_expiredToken_throwsBadCredentialsException() {
        OneTimeToken ott = new OneTimeToken(1L, "old-token", LocalDate.now().minusDays(1));
        when(repo.findByToken("old-token")).thenReturn(Optional.of(ott));

        assertThrows(BadCredentialsException.class, () ->
                provider.authenticate(auth("old-token")));
    }

    @Test
    void authenticate_futureToken_throwsBadCredentialsException() {
        // A token valid "tomorrow" should also be rejected (not today)
        OneTimeToken ott = new OneTimeToken(1L, "future-token", LocalDate.now().plusDays(1));
        when(repo.findByToken("future-token")).thenReturn(Optional.of(ott));

        assertThrows(BadCredentialsException.class, () ->
                provider.authenticate(auth("future-token")));
    }

    // -------------------------------------------------------------------------
    // supports()
    // -------------------------------------------------------------------------

    @Test
    void supports_usernamePasswordToken_returnsTrue() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }

    @Test
    void supports_otherClass_returnsFalse() {
        assertThat(provider.supports(Object.class)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Authentication auth(String token) {
        return new UsernamePasswordAuthenticationToken(token, "");
    }
}
