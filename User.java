package engine;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import javax.sql.DataSource;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "AppUsers")
public class User implements UserDetails {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Email(regexp = ".+@.+\\..+", message = "Invalid email format")
    @JsonProperty(value = "email")
    private String username;

    @Size(min = 5, message = "The password must have at least 5 characters")
    private String password;

    private final boolean enabled = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIdentityInfo(generator= ObjectIdGenerators.PropertyGenerator.class, property="id")
    @JsonIdentityReference(alwaysAsId=true)
    private List<Quiz> quizzes = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public long getId() {
        return this.id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter {

    /*@Autowired
    DataSource dataSource;*/

    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth)
                                                    throws Exception {
        auth
                .userDetailsService(this.userDetailsService)
                .passwordEncoder(encoder());
    }

    @Override
    protected void configure(HttpSecurity http)
                                        throws Exception {
        String[] permittedPaths
                = new String[]{"/api/register","/actuator/shutdown"};
        String[] authenticatedPaths
                = new String[]{"/api/quizzes", "/api/quizzes/**"};
        http.httpBasic().and()
                .authorizeRequests()
                    .antMatchers(authenticatedPaths)
                        .fullyAuthenticated()
                    .antMatchers(permittedPaths)
                        .permitAll();
        // DISABLE ON PRODUCTION!
        http.csrf().disable();
        http.headers().frameOptions().disable();
    }
}

@Service
class UserRepositoryUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;
    private final String USER_NOT_FOUND = "User %s doesn't exist";

    @Autowired
    public UserRepositoryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
                                        throws UsernameNotFoundException {
        User user = this.userRepository.findByUsername(username);
        if (user != null) return user;
        else throw new UsernameNotFoundException(String.format(USER_NOT_FOUND, username));
    }
}

@RestController
class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @PostMapping(value = "/api/register", consumes = "application/json")
    public ResponseEntity<String> registerUser(@Validated @RequestBody User newUser,
                                                BindingResult bindingResult) throws Exception {

        if (bindingResult.hasErrors()) {
            List<FieldError> bindingErrors = bindingResult.getFieldErrors();
            StringBuilder errorOutput = new StringBuilder();
            for (FieldError error : bindingErrors) {
                if (error.toString().matches(".*username.*")) {
                    errorOutput.append("username");
                } else if (error.toString().matches(".*password.*"))
                    errorOutput.append(", password");
            }
            throw new InvalidUserDataException(
                    "Register data is incorrect. Check the following field(s): "
                            + errorOutput);
        }
        if (userRepository.findByUsername(newUser.getUsername()) != null)
            return new ResponseEntity<>("Your email has already been registered", HttpStatus.BAD_REQUEST);
        newUser.setPassword(
                    this.encoder
                        .encode(
                                newUser.getPassword()));
        this.userRepository.save(newUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}

@Repository
interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);

}

