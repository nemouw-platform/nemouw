package com.nemouw.syndicate.staff;

import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.nemouw.syndicate.staff.StaffRows.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SyndicateOwnersTest {

    private final SyndicateStaffRepository staff = mock(SyndicateStaffRepository.class);
    private final SyndicateStaffAssembler assembler = mock(SyndicateStaffAssembler.class);
    private final SyndicateOwners owners = new SyndicateOwners(staff, assembler);

    @Test
    void registersTheOwnerAndReturnsTheAssembledRow() {
        SyndicateStaffRow row = StaffRows.owner();
        when(staff.insertOwner(SYNDICATE_ID, OWNER_USER_ID)).thenReturn(OWNER_STAFF_ID);
        when(staff.findInSyndicate(OWNER_STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(row));
        when(assembler.toResponse(row)).thenReturn(StaffRows.response(row));

        SyndicateStaffResponse response = owners.addOwner(SYNDICATE_ID, OWNER_USER_ID);

        verify(staff).insertOwner(SYNDICATE_ID, OWNER_USER_ID);
        assertThat(response.id()).isEqualTo(OWNER_STAFF_ID);
        assertThat(response.owner()).isTrue();
    }

    @Test
    void findsTheCurrentOwner() {
        SyndicateStaffRow row = StaffRows.owner();
        when(staff.findOwner(SYNDICATE_ID)).thenReturn(Optional.of(row));
        when(assembler.toResponse(row)).thenReturn(StaffRows.response(row));

        assertThat(owners.findOwner(SYNDICATE_ID)).hasValueSatisfying(owner -> assertThat(owner.owner()).isTrue());
    }

    @Test
    void findsNoOwnerForASyndicateWithoutOne() {
        assertThat(owners.findOwner(SYNDICATE_ID)).isEmpty();
    }
}