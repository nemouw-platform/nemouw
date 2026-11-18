package com.nemouw.syndicate.staff;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import com.nemouw.user.api.UserDirectory;
import com.nemouw.user.api.UserSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nemouw.syndicate.staff.StaffRows.SYNDICATE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SyndicateStaffAssemblerTest {

    private static final UUID ID_A = UUID.fromString("0f3e9b2a-5c71-4d88-b6a4-21d7e8c05f93");
    private static final UUID ID_B = UUID.fromString("3d6a1f80-92b4-4e17-a5c3-8b0e7d2f4961");
    private static final UUID USER_A = UUID.fromString("7b92d4e1-0a63-4c57-9e18-f4a05c6b3d72");
    private static final UUID USER_B = UUID.fromString("e41b8c07-6d29-4f53-8a90-1c5d3b7e2a68");
    private static final OffsetDateTime CREATED_A = OffsetDateTime.parse("2026-02-01T09:30:00Z");
    private static final OffsetDateTime CREATED_B = OffsetDateTime.parse("2026-03-05T14:00:00Z");

    private final UserDirectory users = mock(UserDirectory.class);
    private final SyndicateStaffAssembler assembler = new SyndicateStaffAssembler(users);

    private static UserSummary summary(UUID id, String firstName) {
        return new UserSummary(id, firstName, "Khoury", firstName.toLowerCase() + "@order.org",
                UserRole.SYNDICATE_STAFF, UserStatus.ACTIVE, Instant.parse("2026-01-15T08:00:00Z"));
    }

    @Test
    void mapsEveryFieldOfEveryRowWithOneBatchedUserLookupInRowOrder() {
        SyndicateStaffRow rowA = new SyndicateStaffRow(ID_A, SYNDICATE_ID, USER_A, "Registrar", false, CREATED_A);
        SyndicateStaffRow rowB = new SyndicateStaffRow(ID_B, SYNDICATE_ID, USER_B, null, true, CREATED_B);
        UserSummary layla = summary(USER_A, "Layla");
        UserSummary omar = summary(USER_B, "Omar");
        when(users.summaries(List.of(USER_A, USER_B))).thenReturn(Map.of(USER_A, layla, USER_B, omar));

        List<SyndicateStaffResponse> responses = assembler.toResponses(List.of(rowA, rowB));

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().id()).isEqualTo(ID_A);
        assertThat(responses.getFirst().syndicateId()).isEqualTo(SYNDICATE_ID);
        assertThat(responses.getFirst().user()).isSameAs(layla);
        assertThat(responses.getFirst().title()).isEqualTo("Registrar");
        assertThat(responses.getFirst().owner()).isFalse();
        assertThat(responses.getFirst().createdAt()).isEqualTo(CREATED_A.toInstant());
        assertThat(responses.get(1).id()).isEqualTo(ID_B);
        assertThat(responses.get(1).user()).isSameAs(omar);
        assertThat(responses.get(1).title()).isNull();
        assertThat(responses.get(1).owner()).isTrue();
        assertThat(responses.get(1).createdAt()).isEqualTo(CREATED_B.toInstant());
        verify(users).summaries(List.of(USER_A, USER_B));
        verifyNoMoreInteractions(users);
    }

    @Test
    void mapsASingleRow() {
        SyndicateStaffRow row = new SyndicateStaffRow(ID_A, SYNDICATE_ID, USER_A, "Registrar", false, CREATED_A);
        when(users.summaries(List.of(USER_A))).thenReturn(Map.of(USER_A, summary(USER_A, "Layla")));

        assertThat(assembler.toResponse(row).id()).isEqualTo(ID_A);
    }

    @Test
    void returnsNothingForNoRows() {
        assertThat(assembler.toResponses(List.of())).isEmpty();
    }

    @Test
    void failsLoudlyWhenARowHasNoMatchingUser() {
        SyndicateStaffRow row = new SyndicateStaffRow(ID_A, SYNDICATE_ID, USER_A, "Registrar", false, CREATED_A);
        when(users.summaries(List.of(USER_A))).thenReturn(Map.of());

        assertThatThrownBy(() -> assembler.toResponse(row))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ID_A.toString())
                .hasMessageContaining(USER_A.toString());
    }
}