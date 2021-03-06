package service;

import configuration.security.UserDetailsServiceImplementation;
import controllers.customer.AuthenticatedCustomerInformationDto;
import controllers.customer.NewCustomerInformationDto;
import controllers.exceptions.AuthorizationException;
import model.*;
import model.type.CustomerType;
import model.type.FuelType;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.CustomerRepository;
import repository.CustomerTypeRepository;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class CustomerService {
    private static final Logger log = Logger.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserDetailsServiceImplementation userDetailsServiceImplementation;
    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    public Customer processCustomerLogin(HttpServletResponse response, String customerLogin) {
        Customer customer = customerRepository.getByLogin(customerLogin);
        int numberOfEntries;
        ObjectMapper objectMapper = new ObjectMapper();
        AuthenticatedCustomerInformationDto authenticatedCustomerInformationDto = new AuthenticatedCustomerInformationDto();

        String customerType = customer.getCustomerType().getTypeName();
        numberOfEntries = "REGULAR".equals(customerType) ? customer.getVehicles().size() : customer.getGasstations().size();
        authenticatedCustomerInformationDto.setCustomerType(customerType);
        authenticatedCustomerInformationDto.setLogin(customer.getLogin());
        authenticatedCustomerInformationDto.setNumberOfEntries(numberOfEntries);
        try {
            response.getWriter().print(objectMapper.writeValueAsString(authenticatedCustomerInformationDto));
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return customer;
    }

    public List<CustomerType> getAllCustomerTypes() {
        return customerTypeRepository.findAll();
    }

    public void registerANewCustomer(NewCustomerInformationDto newCustomerInformationDto) {
        Customer customer = new Customer();

        if (customerRepository.checkIfLoginExistsInDatabase(newCustomerInformationDto.getLogin())) {
            throw new AuthorizationException("Such login is in data base. Choose another one");
        }

        if (customerRepository.checkIfEmailExistsInDatabase(newCustomerInformationDto.getEmail())) {
            throw new AuthorizationException("Such email address is in data base. Choose another one.");
        }

        customer.setFirstName(newCustomerInformationDto.getFirstName());
        customer.setLastName(newCustomerInformationDto.getLastName());
        customer.setDateOfBirth(newCustomerInformationDto.getDateOfBirth());
        customer.setLogin(newCustomerInformationDto.getLogin());
        customer.setPassword(newCustomerInformationDto.getPassword());
        customer.setEmail(newCustomerInformationDto.getEmail());
        customer.setGender("Male".equals(newCustomerInformationDto.getGender()) ?
                Gender.MALE : Gender.FEMALE);
        customer.setCustomerType("REGULAR".equals(newCustomerInformationDto.getGroup()) ?
                customerTypeRepository.getByName("REGULAR") : customerTypeRepository.getByName("BUSINESS"));
        customer.setEnabled(true);
        customer.setPassword(bCryptPasswordEncoder.encode(newCustomerInformationDto.getPassword()));

        log.info(String.format("About to register a new customer: %s", customer.toString()));
        customerRepository.save(customer);

        UserDetails userDetails = userDetailsServiceImplementation.loadUserByUsername(customer.getLogin());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(),
                        userDetails.getPassword(),
                        userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
