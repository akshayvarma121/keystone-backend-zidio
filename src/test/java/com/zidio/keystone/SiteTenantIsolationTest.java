package com.zidio.keystone;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.Site;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.SiteRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.JwtService;
import com.zidio.keystone.security.KeystoneUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SiteTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private UserRepository userRepository;

    private String customerAToken;
    private UUID siteAId;
    private UUID siteBId;

    @BeforeEach
    void setup() {
        siteRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Customer A
        Customer customerA = new Customer();
        customerA.setName("Customer A");
        customerA.setContactEmail("a@a.com");
        customerA = customerRepository.save(customerA);

        Site siteA = new Site();
        siteA.setCustomer(customerA);
        siteA.setName("Site A");
        siteA = siteRepository.save(siteA);
        siteAId = siteA.getId();

        User userA = new User();
        userA.setName("User A");
        userA.setEmail("userA@keystone.local");
        userA.setPasswordHash("hash");
        userA.setRole(Role.CUSTOMER);
        userA.setCustomer(customerA);
        userA = userRepository.save(userA);

        // Customer B
        Customer customerB = new Customer();
        customerB.setName("Customer B");
        customerB.setContactEmail("b@b.com");
        customerB = customerRepository.save(customerB);

        Site siteB = new Site();
        siteB.setCustomer(customerB);
        siteB.setName("Site B");
        siteB = siteRepository.save(siteB);
        siteBId = siteB.getId();

        // Generate JWT for User A
        KeystoneUserDetails userDetailsA = new KeystoneUserDetails(
                userA.getId(), userA.getEmail(), userA.getPasswordHash(),
                userA.getRole(), customerA.getId(), true
        );
        customerAToken = jwtService.generateToken(userDetailsA);
    }

    @Test
    void customerA_CanOnlySeeOwnSitesInList() throws Exception {
        mockMvc.perform(get("/api/sites")
                .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Site A"));
    }

    @Test
    void customerA_CannotFetchCustomerBSiteById() throws Exception {
        mockMvc.perform(get("/api/sites/" + siteBId)
                .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
