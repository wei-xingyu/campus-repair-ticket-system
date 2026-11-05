package com.weixingyu.campusrepairticketsystem;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import com.weixingyu.campusrepairticketsystem.controller.*;
import com.weixingyu.campusrepairticketsystem.Exception.GlobalException;
import com.weixingyu.campusrepairticketsystem.service.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
class RequestValidationMigrationTest {
 @Test void allCompletedBodyEndpointsRejectMissingRequiredFields()throws Exception{
 Object[][] cases={
 {new LoginController(),"loginService",LoginService.class,"POST","/auth/login"},
 {new BuildingController(),"buildingService",BuildingService.class,"POST","/admin/buildings"},
 {new BuildingController(),"buildingService",BuildingService.class,"PUT","/admin/buildings/1"},
 {new BuildingController(),"buildingService",BuildingService.class,"PUT","/admin/buildings/1/status"},
 {new BasicDataController(),"basicDataService",BasicDataService.class,"POST","/admin/categories"},
 {new BasicDataController(),"basicDataService",BasicDataService.class,"PUT","/admin/categories/1"},
 {new BasicDataController(),"basicDataService",BasicDataService.class,"PUT","/admin/categories/1/status"},
 {new AdminAssetController(),"adminAssetService",AdminAssetService.class,"POST","/admin/assets"},
 {new AdminAssetController(),"adminAssetService",AdminAssetService.class,"PUT","/admin/assets/1"},
 {new AdminAssetController(),"adminAssetService",AdminAssetService.class,"PUT","/admin/assets/1/status"},
 {new AdminRepairerController(),"adminRepairerService",AdminRepairerService.class,"POST","/admin/repairers"},
 {new AdminRepairerController(),"adminRepairerService",AdminRepairerService.class,"PUT","/admin/repairers/1/status"}};
 for(Object[] c:cases){Object service=mock((Class<?>)c[2]);ReflectionTestUtils.setField(c[0],(String)c[1],service);
 var mvc=MockMvcBuilders.standaloneSetup(c[0]).setControllerAdvice(new GlobalException()).build();
 var req="POST".equals(c[3])?post((String)c[4]):put((String)c[4]);
 var result=mvc.perform(req.contentType("application/json").content("{}")).andReturn();
 assertEquals(400,result.getResponse().getStatus(),(String)c[4]);assertTrue(result.getResponse().getContentAsString().contains("\"code\":0"));verifyNoInteractions(service);
 }
 }
 @Test void categoryStatusOnlyNeedsEnabled()throws Exception{
 var service=mock(BasicDataService.class);var controller=new BasicDataController();ReflectionTestUtils.setField(controller,"basicDataService",service);
 var mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalException()).build();
 assertEquals(200,mvc.perform(put("/admin/categories/1/status").contentType("application/json").content("{\"enabled\":false}")).andReturn().getResponse().getStatus());
 verify(service).updateCategoryStatus(eq(1L),argThat(r->Boolean.FALSE.equals(r.getEnabled())));
 }
 @Test void attachmentAndPaginationBoundaries(){
  try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
   var validator=factory.getValidator();
   var request=new com.weixingyu.campusrepairticketsystem.pojo.request.CreateTacketsRequest();
   request.setTitle("报修标题");request.setDescription("这里是故障详细描述");request.setRoom("101");request.setCategoryId(1L);request.setBuildingId(1L);
   assertTrue(validator.validate(request).isEmpty());request.setAttachmentUrls(new String[6]);assertFalse(validator.validate(request).isEmpty());request.setAttachmentUrls(new String[5]);assertTrue(validator.validate(request).isEmpty());
   var query=new com.weixingyu.campusrepairticketsystem.pojo.request.QueryTicketsRequest();assertTrue(validator.validate(query).isEmpty());query.setPageSize(101);assertFalse(validator.validate(query).isEmpty());query.setPageSize(100);query.setStatus("INVALID");assertFalse(validator.validate(query).isEmpty());
  }
 }
}
