package com.nemouw.syndicate.staff;

import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateOwnerChangeRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.nemouw.support.Fixtures.assertApiError;
import static com.nemouw.syndicate.staff.StaffRows.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyndicateOwnerServiceTest {

    private final SyndicateStaffRepository staff = mock(SyndicateStaffRepository.class);
    private final SyndicateStaffAssembler assembler = mock(SyndicateStaffAssembler.class);
    private final SyndicateOwnerService service = new SyndicateOwnerService(staff, assembler);

    private static SyndicateOwnerChangeRequest request() {
        return new SyndicateOwnerChangeRequest(STAFF_ID);
    }

    private void assertNothingTransferred() {
        verify(staff, never()).transferOwnership(any(), any());
    }

    @Test
    void requiresAStaffId() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.changeOwner(SYNDICATE_ID, new SyndicateOwnerChangeRequest(null)));

        assertThat(thrown).isInstanceOf(ValidationException.class);
        assertThat(((ValidationException) thrown).fieldErrors()).containsExactly(FieldErrors.required("staff_id"));
        verify(staff, never()).findInSyndicate(any(), any());
        assertNothingTransferred();
    }

    @Test
    void answersNotFoundBeforeValidatingTheBody() {
        assertApiError(() -> service.changeOwner(SYNDICATE_ID, new SyndicateOwnerChangeRequest(null)),
                ErrorCode.NOT_FOUND);
        assertNothingTransferred();
    }

    @Test
    void answersNotFoundForAnUnknownSyndicate() {
        assertApiError(() -> service.changeOwner(SYNDICATE_ID, request()), ErrorCode.NOT_FOUND);
        assertNothingTransferred();
    }

    @Test
    void answersNotFoundForAStaffMemberOfAnotherSyndicate() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);

        assertApiError(() -> service.changeOwner(SYNDICATE_ID, request()), ErrorCode.NOT_FOUND);
        assertNothingTransferred();
    }

    @Test
    void refusesToHandOwnershipToTheCurrentOwner() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(staff.findInSyndicate(STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(StaffRows.owner()));

        assertApiError(() -> service.changeOwner(SYNDICATE_ID, request()), ErrorCode.ALREADY_OWNER);
        assertNothingTransferred();
    }

    @Test
    void answersNotFoundWhenTheSyndicateHasNoCurrentOwner() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(staff.findInSyndicate(STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(StaffRows.member()));

        assertApiError(() -> service.changeOwner(SYNDICATE_ID, request()), ErrorCode.NOT_FOUND);
        assertNothingTransferred();
    }

    @Test
    void handsOwnershipOverAndReturnsTheNewOwnersFreshRow() {
        SyndicateStaffRow next = StaffRows.member();
        SyndicateStaffRow promoted = new SyndicateStaffRow(STAFF_ID, SYNDICATE_ID, STAFF_USER_ID, "Registrar", true,
                next.createdAt());
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(staff.findInSyndicate(STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(next))
                .thenReturn(Optional.of(promoted));
        when(staff.findOwner(SYNDICATE_ID)).thenReturn(Optional.of(StaffRows.owner()));
        when(assembler.toResponse(promoted)).thenReturn(StaffRows.response(promoted));

        SyndicateStaffResponse response = service.changeOwner(SYNDICATE_ID, request());

        verify(staff).transferOwnership(OWNER_STAFF_ID, STAFF_ID);
        assertThat(response.id()).isEqualTo(STAFF_ID);
        assertThat(response.owner()).isTrue();
    }

    @Test
    void keepsThePreviousOwnerAsStaffAndLeavesEveryTitleAlone() {
        SyndicateStaffRow next = StaffRows.member();
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(staff.findInSyndicate(STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(next));
        when(staff.findOwner(SYNDICATE_ID)).thenReturn(Optional.of(StaffRows.owner()));
        when(assembler.toResponse(next)).thenReturn(StaffRows.response(next));

        service.changeOwner(SYNDICATE_ID, request());

        verify(staff, never()).delete(any());
        verify(staff, never()).updateTitle(any(), any(), any());
    }
}