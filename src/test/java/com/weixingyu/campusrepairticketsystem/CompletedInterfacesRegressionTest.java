package com.weixingyu.campusrepairticketsystem;

import com.weixingyu.campusrepairticketsystem.mapper.*;
import com.weixingyu.campusrepairticketsystem.pojo.request.*;
import com.weixingyu.campusrepairticketsystem.pojo.response.*;
import com.weixingyu.campusrepairticketsystem.pojo.entity.*;
import com.weixingyu.campusrepairticketsystem.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompletedInterfacesRegressionTest {
    @Test void buildingWithoutAddressReturnsGeneratedId() {
        BuildingMapper mapper = mock(BuildingMapper.class);
        BuildingServiceImpl service = new BuildingServiceImpl();
        ReflectionTestUtils.setField(service, "buildingMapper", mapper);
        AdminBuildRequest request = new AdminBuildRequest(null, "教学楼", null, null);
        doAnswer(call -> { ((AdminBuildRequest)call.getArgument(0)).setId(77L); return null; }).when(mapper).insert(any());
        Result result = service.createBuilding(request);
        assertEquals(77L, ((AdminBuildRequest)result.getData()).getId());
        verify(mapper, never()).selectId(any());
    }
    @Test void missingRepairerFieldsAreRejectedBeforeDatabaseAccess() {
        AdminRepairerMapper mapper = mock(AdminRepairerMapper.class);
        AdminRepairerServiceImpl service = new AdminRepairerServiceImpl();
        ReflectionTestUtils.setField(service, "adminRepairerMapper", mapper);
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new CreateRepairerRequest()).isEmpty());
            assertFalse(factory.getValidator().validate(new UpdateStatusRequest()).isEmpty());
        }
        verifyNoInteractions(mapper);
    }
    @Test void staleAssignmentReturns409WithoutWriting() {
        TacketAdminMapper mapper = mock(TacketAdminMapper.class);
        AdminRepairerMapper repairers = mock(AdminRepairerMapper.class);
        TacketAdminServiceImpl service = new TacketAdminServiceImpl();
        ReflectionTestUtils.setField(service, "tacketAdminMapper", mapper);
        ReflectionTestUtils.setField(service, "adminRepairerMapper", repairers);
        when(repairers.findEnabledRepairerById(2L)).thenReturn(new SysUser());
        Ticket ticket = new Ticket(); ticket.setId(1L); ticket.setVersion(3); ticket.setStatus("PENDING_ASSIGNMENT");
        when(mapper.selectTicketById(1L)).thenReturn(ticket);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.adminDispatch(1L, new AdminDispatchRequest(2L, null, 2), new LoginResponse()));
        assertEquals(409, error.getStatusCode().value());
        verify(mapper, never()).assignTicket(any(), any(), any());
    }
    @Test void oversizedAssetCodeIsRejected() {
        AdminAssetMapper mapper = mock(AdminAssetMapper.class);
        AdminAssetServiceImpl service = new AdminAssetServiceImpl();
        ReflectionTestUtils.setField(service, "adminAssetMapper", mapper);
        AssetRequest request = new AssetRequest(null, 1L, "a".repeat(65), "设备", null, true);
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
        verifyNoInteractions(mapper);
    }

    @Test void assignmentWritesNewVersionAndPreviousAssigneeToHistory() {
        TacketAdminMapper mapper = mock(TacketAdminMapper.class);
        AdminRepairerMapper repairers = mock(AdminRepairerMapper.class);
        TacketAdminServiceImpl service = new TacketAdminServiceImpl();
        ReflectionTestUtils.setField(service, "tacketAdminMapper", mapper);
        ReflectionTestUtils.setField(service, "adminRepairerMapper", repairers);
        when(repairers.findEnabledRepairerById(2L)).thenReturn(new SysUser());
        Ticket oldTicket = new Ticket(); oldTicket.setId(1L); oldTicket.setVersion(0); oldTicket.setStatus("PENDING_ASSIGNMENT");
        Ticket updated = new Ticket(); updated.setVersion(1);
        when(mapper.selectTicketById(1L)).thenReturn(oldTicket, updated);
        when(mapper.assignTicket(1L, 2L, 0)).thenReturn(1);
        when(mapper.insertAssignmentHistory(1L, 1, "PENDING_ASSIGNMENT", "PENDING_ACCEPTANCE", 9L, null, 2L, "说明")).thenReturn(1);
        assertEquals(1, service.adminDispatch(1L, new AdminDispatchRequest(2L, "说明", 0), new LoginResponse(9L, "admin", "管理员", "ADMIN")).getCode());
        verify(mapper).insertAssignmentHistory(1L, 1, "PENDING_ASSIGNMENT", "PENDING_ACCEPTANCE", 9L, null, 2L, "说明");
    }
    @Test void creationHistoryFailureThrowsInsteadOfReturningSuccess() {
        TacketAdminMapper mapper = mock(TacketAdminMapper.class);
        TacketAdminServiceImpl service = new TacketAdminServiceImpl();
        ReflectionTestUtils.setField(service, "tacketAdminMapper", mapper);
        when(mapper.findEnabledCategoryName(1L)).thenReturn("水电");
        when(mapper.findEnabledBuildingName(1L)).thenReturn("教学楼");
        when(mapper.getDailySequence(any())).thenReturn(1);
        when(mapper.insertTicket(any())).thenAnswer(call -> { ((Ticket)call.getArgument(0)).setId(3L); return 1; });
        when(mapper.insertCreationHistory(3L, 9L)).thenReturn(0);
        CreateTacketsRequest request = new CreateTacketsRequest();
        request.setTitle("水管漏水"); request.setDescription("教学楼水管持续漏水"); request.setRoom("101"); request.setCategoryId(1L); request.setBuildingId(1L);
        assertThrows(IllegalStateException.class, () -> service.createTackets(request, new LoginResponse(9L, "student", "学生", "STUDENT")));
        verify(mapper).insertCreationHistory(3L, 9L);
    }
}
