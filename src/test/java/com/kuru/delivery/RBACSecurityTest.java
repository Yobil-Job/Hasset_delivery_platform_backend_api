/*
 * package com.kuru.delivery;
 * 
 * import org.junit.jupiter.api.Test; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
 * import org.springframework.boot.test.context.SpringBootTest; import
 * org.springframework.http.MediaType; import
 * org.springframework.security.test.context.support.WithMockUser; import
 * org.springframework.test.web.servlet.MockMvc;
 * 
 * import static org.springframework.security.test.web.servlet.request.
 * SecurityMockMvcRequestPostProcessors.csrf; import static
 * org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import
 * static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * 
 *//**
	 * RBAC Security Tests
	 * 
	 * Tests that verify role-based access control is properly enforced: - Customers
	 * cannot access admin endpoints - Drivers cannot access admin endpoints -
	 * Admins can access admin endpoints - Users can only access their own resources
	 *//*
		 * @SpringBootTest
		 * 
		 * @AutoConfigureMockMvc public class RBACSecurityTest {
		 * 
		 * @Autowired private MockMvc mockMvc;
		 * 
		 * // ========== ADMIN ENDPOINT TESTS ==========
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "CUSTOMER") public void
		 * testCustomerCannotAccessAdminEndpoints() throws Exception { // Test admin
		 * customer endpoint mockMvc.perform(get("/api/admin/customers") .with(csrf()))
		 * .andExpect(status().isForbidden());
		 * 
		 * // Test admin driver endpoint mockMvc.perform(get("/api/admin/drivers")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin order endpoint mockMvc.perform(get("/api/admin/orders")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin FAQ endpoint mockMvc.perform(get("/api/admin/faqs")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin pricing endpoint
		 * mockMvc.perform(get("/api/admin/pricing/config") .with(csrf()))
		 * .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "DRIVER") public void
		 * testDriverCannotAccessAdminEndpoints() throws Exception { // Test admin
		 * customer endpoint mockMvc.perform(get("/api/admin/customers") .with(csrf()))
		 * .andExpect(status().isForbidden());
		 * 
		 * // Test admin driver endpoint mockMvc.perform(get("/api/admin/drivers")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin order endpoint mockMvc.perform(get("/api/admin/orders")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin FAQ endpoint mockMvc.perform(get("/api/admin/faqs")
		 * .with(csrf())) .andExpect(status().isForbidden());
		 * 
		 * // Test admin pricing endpoint
		 * mockMvc.perform(get("/api/admin/pricing/config") .with(csrf()))
		 * .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "ADMIN") public void testAdminCanAccessAdminEndpoints()
		 * throws Exception { // Note: These will return 200 or 404 depending on data,
		 * but should NOT return 403 // Test admin customer endpoint
		 * mockMvc.perform(get("/api/admin/customers") .with(csrf()))
		 * .andExpect(status().isOk().or(status().isNotFound()));
		 * 
		 * // Test admin driver endpoint mockMvc.perform(get("/api/admin/drivers")
		 * .with(csrf())) .andExpect(status().isOk().or(status().isNotFound()));
		 * 
		 * // Test admin order endpoint mockMvc.perform(get("/api/admin/orders")
		 * .with(csrf())) .andExpect(status().isOk().or(status().isNotFound()));
		 * 
		 * // Test admin FAQ endpoint mockMvc.perform(get("/api/admin/faqs")
		 * .with(csrf())) .andExpect(status().isOk().or(status().isNotFound())); }
		 * 
		 * // ========== CUSTOMER ENDPOINT TESTS ==========
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "DRIVER") public void
		 * testDriverCannotAccessCustomerEndpoints() throws Exception { // Test customer
		 * profile endpoint mockMvc.perform(get("/api/customers/profile") .with(csrf()))
		 * .andExpect(status().isForbidden());
		 * 
		 * // Test customer orders endpoint mockMvc.perform(get("/api/orders/my-orders")
		 * .with(csrf())) .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "CUSTOMER") public void
		 * testCustomerCanAccessCustomerEndpoints() throws Exception { // Note: These
		 * will return 200 or 404 depending on data, but should NOT return 403 // Test
		 * customer profile endpoint mockMvc.perform(get("/api/customers/profile")
		 * .with(csrf())) .andExpect(status().isOk().or(status().isNotFound()));
		 * 
		 * // Test customer orders endpoint mockMvc.perform(get("/api/orders/my-orders")
		 * .with(csrf())) .andExpect(status().isOk().or(status().isNotFound())); }
		 * 
		 * // ========== DRIVER ENDPOINT TESTS ==========
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "CUSTOMER") public void
		 * testCustomerCannotAccessDriverEndpoints() throws Exception { // Test driver
		 * profile endpoint mockMvc.perform(get("/api/drivers/me") .with(csrf()))
		 * .andExpect(status().isForbidden());
		 * 
		 * // Test driver location update endpoint
		 * mockMvc.perform(post("/api/driver/location/update")
		 * .contentType(MediaType.APPLICATION_JSON)
		 * .content("{\"latitude\": 9.0, \"longitude\": 38.0}") .with(csrf()))
		 * .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "DRIVER") public void
		 * testDriverCanAccessDriverEndpoints() throws Exception { // Note: These will
		 * return 200 or 404 depending on data, but should NOT return 403 // Test driver
		 * profile endpoint mockMvc.perform(get("/api/drivers/me") .with(csrf()))
		 * .andExpect(status().isOk().or(status().isNotFound())); }
		 * 
		 * // ========== UNAUTHENTICATED ACCESS TESTS ==========
		 * 
		 * @Test public void testUnauthenticatedUserCannotAccessProtectedEndpoints()
		 * throws Exception { // Test admin endpoint
		 * mockMvc.perform(get("/api/admin/customers"))
		 * .andExpect(status().isUnauthorized().or(status().isForbidden()));
		 * 
		 * // Test customer endpoint mockMvc.perform(get("/api/customers/profile"))
		 * .andExpect(status().isUnauthorized().or(status().isForbidden()));
		 * 
		 * // Test driver endpoint mockMvc.perform(get("/api/drivers/me"))
		 * .andExpect(status().isUnauthorized().or(status().isForbidden())); }
		 * 
		 * // ========== PAYMENT ENDPOINT TESTS ==========
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "DRIVER") public void
		 * testDriverCannotAccessCustomerPaymentEndpoints() throws Exception {
		 * mockMvc.perform(post("/api/payments/initialize")
		 * .contentType(MediaType.APPLICATION_JSON) .content("{\"orderId\": 1}")
		 * .with(csrf())) .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "CUSTOMER") public void
		 * testCustomerCannotAccessAdminPaymentEndpoints() throws Exception {
		 * mockMvc.perform(get("/api/payments/admin") .with(csrf()))
		 * .andExpect(status().isForbidden()); }
		 * 
		 * @Test
		 * 
		 * @WithMockUser(roles = "ADMIN") public void
		 * testAdminCanAccessAdminPaymentEndpoints() throws Exception {
		 * mockMvc.perform(get("/api/payments/admin") .with(csrf()))
		 * .andExpect(status().isOk().or(status().isNotFound())); } }
		 * 
		 */