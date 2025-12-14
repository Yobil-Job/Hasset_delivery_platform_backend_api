/*
 * package com.kuru.delivery;
 * 
 * import com.fasterxml.jackson.databind.ObjectMapper; import
 * com.kuru.delivery.auth.dto.RegisterRequest; import
 * com.kuru.delivery.driver.model.Driver; import
 * com.kuru.delivery.driver.model.DriverStatus; import
 * com.kuru.delivery.driver.repository.DriverRepository; import
 * com.kuru.delivery.user.model.Role; import
 * com.kuru.delivery.user.repository.UserRepository; import
 * org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
 * import org.springframework.boot.test.context.SpringBootTest; import
 * org.springframework.http.MediaType; import
 * org.springframework.security.test.context.support.WithMockUser; import
 * org.springframework.test.web.servlet.MockMvc;
 * 
 * import static org.junit.jupiter.api.Assertions.assertEquals; import static
 * org.junit.jupiter.api.Assertions.assertNotNull; import static
 * org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
 * import static
 * org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
 * import static
 * org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 * 
 * @SpringBootTest
 * 
 * @AutoConfigureMockMvc class DriverOnboardingTest {
 * 
 * @Autowired private MockMvc mockMvc;
 * 
 * @Autowired private UserRepository userRepository;
 * 
 * @Autowired private DriverRepository driverRepository;
 * 
 * @Autowired private ObjectMapper objectMapper;
 * 
 * @BeforeEach void setUp() { driverRepository.deleteAll();
 * userRepository.deleteAll(); }
 * 
 * @Test void testDriverRegistrationAndApprovalFlow() throws Exception { // 1.
 * Register Driver RegisterRequest registerRequest = new RegisterRequest();
 * registerRequest.setFirstname("John"); registerRequest.setLastname("Doe");
 * registerRequest.setEmail("driver@test.com");
 * registerRequest.setPassword("password");
 * registerRequest.setRole(Role.DRIVER); // This might be ignored by
 * DriverController but good to set
 * 
 * mockMvc.perform(post("/api/drivers/register")
 * .contentType(MediaType.APPLICATION_JSON)
 * .content(objectMapper.writeValueAsString(registerRequest)))
 * .andExpect(status().isOk());
 * 
 * // 2. Verify Driver Created and Pending Driver driver =
 * driverRepository.findByUserId(userRepository.findByEmail("driver@test.com").
 * get().getId()).orElseThrow(); assertEquals(DriverStatus.PENDING,
 * driver.getStatus()); assertEquals(Role.DRIVER, driver.getUser().getRole());
 * 
 * 
 * 
 * // 4. Suspend Driver (as Admin) mockMvc.perform(put("/api/admin/drivers/" +
 * driver.getId() + "/suspend") .with(user -> { user.setRoles("ADMIN"); return
 * user; })) .andExpect(status().isOk());
 * 
 * // Re-fetch driver = driverRepository.findById(driver.getId()).orElseThrow();
 * assertEquals(DriverStatus.SUSPENDED, driver.getStatus()); }
 * 
 * @Test
 * 
 * @WithMockUser(roles = "CUSTOMER") void testCustomerCannotApproveDriver()
 * throws Exception { // 1. Register Driver RegisterRequest registerRequest =
 * new RegisterRequest(); registerRequest.setFirstname("Jane");
 * registerRequest.setLastname("Doe");
 * registerRequest.setEmail("jane@test.com");
 * registerRequest.setPassword("password");
 * 
 * mockMvc.perform(post("/api/drivers/register")
 * .contentType(MediaType.APPLICATION_JSON)
 * .content(objectMapper.writeValueAsString(registerRequest)))
 * .andExpect(status().isOk());
 * 
 * Driver driver =
 * driverRepository.findByUserId(userRepository.findByEmail("jane@test.com").get
 * ().getId()).orElseThrow();
 * 
 * // 2. Try to approve as Customer mockMvc.perform(put("/api/admin/drivers/" +
 * driver.getId() + "/approve")) .andExpect(status().isForbidden()); } }
 */