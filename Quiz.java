package engine;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Entity
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuizID")
    private long id;

    @NotBlank (message = "Title must not be blank!")
    @NotNull (message = "There must be a title!")
    private String title;

    @Column (name = "task")
    @NotBlank (message = "The question field is blank!")
    @NotNull (message = "There must be a question or task!")
    private String text;

    @NotNull (message = "There must be options provided!")
    @Size(min = 2, max = 4)
    private String[] options;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 4)
    private HashSet<Integer> answer;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIdentityInfo(generator= ObjectIdGenerators.PropertyGenerator.class, property="id")
    @JsonIdentityReference(alwaysAsId=true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private User user;

    public long getId() {
        return id;
    }

    public HashSet<Integer> getAnswer() {
        return answer;
    }
    public String getTitle() {
        return this.title;
    }
    public String getText() {
        return this.text;
    }
    public String[] getOptions() {
        return this.options;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setAnswer(HashSet<Integer> answer) {
        if (answer != null) this.answer = answer;
        else this.answer = new HashSet<>();
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}

class Answer {
    @Size(max = 4)
    private HashSet<Integer> answer;

    public void setAnswer(HashSet<Integer> answer) {
        if (answer != null) this.answer = answer;
        else this.answer = new HashSet<>();
    }

    public HashSet<Integer> getAnswer() {
        return answer;
    }
}

@RestController
@RequestMapping(value = "/api/quizzes")
class QuizController {

    private QuizRepository quizRepository;
    private final String QUIZ_NOT_FOUND = "There is no such quiz!";
    private final String UNAUTHORIZED_ACCESS = "You are not authorized to access this quiz!";
    private final String NO_USER = "There is no authenticated user!";
    private final String SUCCESSFUL_DELETION = "You have successfully deleted the quiz!";
    private UserRepository userRepository;
    private QuizService quizService;
    private SolvedQuizService solvedQuizService;
    private SolvedQuizRepository solvedQuizRepository;

    @Autowired
    QuizController(QuizRepository quizRepository,
                   UserRepository userRepository,
                   QuizService quizService,
                   SolvedQuizService solvedQuizService,
                   SolvedQuizRepository solvedQuizRepository) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.quizService = quizService;
        this.solvedQuizService = solvedQuizService;
        this.solvedQuizRepository = solvedQuizRepository;
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    /*@PersistenceContext
    private EntityManager em;*/

    @GetMapping("/{id}")
    public Quiz getQuiz(@PathVariable("id") long id) throws RuntimeException {
        ResponseEntity<String> response = checkQuizAccessibility(id, false);
        switch(response.getStatusCode()) {
            case OK:
                return quizRepository.getOne(id);
            case NOT_FOUND:
                throw new QuizNotFoundException(QUIZ_NOT_FOUND);
        }
        return null;
    }

    @GetMapping
    public Page<Quiz> getAllQuizzes(@RequestParam(defaultValue = "0") Integer page,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(defaultValue = "id") String sortByField) {
        return this.getQuizzes(page, pageSize, sortByField);
    }

    @GetMapping(value = "/completed")
    public Page<SolvedQuiz> getAllSolvedQuizzesForCurrentUser(@RequestParam(defaultValue = "0") Integer page,
                                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return solvedQuizService.getAllSolvedQuizzesWithPagination(page,
                                                                    pageSize,
                                                                        getAuthenticatedUser().getId());
    }

    @PostMapping(consumes = "application/json")
    public Quiz addQuiz(@Valid @RequestBody Quiz newQuiz) {
        if (newQuiz.getAnswer() == null) newQuiz.setAnswer(new HashSet<>());
        newQuiz.setUser(getAuthenticatedUser());
        quizRepository.save(newQuiz);
        return newQuiz;
    }
    @PostMapping(value = "/{id}/solve", consumes = "application/json")
    public QuizResult checkAnswer(@PathVariable("id") long id,
                                  @Valid @RequestBody Answer answer) throws RuntimeException {
        ResponseEntity<String> response = checkQuizAccessibility(id, false);
        switch(response.getStatusCode()) {
            case OK:
                Quiz solvedQuiz = quizRepository.getOne(id);
                if (solvedQuiz.getAnswer().equals(answer.getAnswer())) {
                    SolvedQuiz newSolvedQuiz = new SolvedQuiz();
                    newSolvedQuiz.setUserId(getAuthenticatedUser().getId());
                    newSolvedQuiz.setQuizId(id);
                    newSolvedQuiz.setCompletedAt(LocalDateTime.now());
                    solvedQuizRepository.save(newSolvedQuiz);
                    return new QuizResult(true);
                }
                else return new QuizResult(false);
            case NOT_FOUND:
                throw new QuizNotFoundException(QUIZ_NOT_FOUND);
        }
        return null;
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteQuiz(@PathVariable("id") long id) throws RuntimeException {
        ResponseEntity<String> response = checkQuizAccessibility(id, true);
        switch(response.getStatusCode()) {
            case OK:
                quizRepository.delete(quizRepository.getOne(id));
                return new ResponseEntity<>(SUCCESSFUL_DELETION, HttpStatus.NO_CONTENT);
            case NOT_FOUND:
                throw new QuizNotFoundException(QUIZ_NOT_FOUND);
            case FORBIDDEN:
                throw new AccessForbiddenException(UNAUTHORIZED_ACCESS);
        }
        return null;
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<Quiz> updateQuiz(@PathVariable("id") long id, @Valid @RequestBody Quiz newQuiz) {
        switch(checkQuizAccessibility(id, true).getStatusCode()) {
            case OK:
                Quiz updatedQuiz = quizRepository.getOne(id);
                updatedQuiz.setUser(getAuthenticatedUser());
                updatedQuiz.setTitle(newQuiz.getTitle());
                updatedQuiz.setText(newQuiz.getText());
                updatedQuiz.setAnswer(newQuiz.getAnswer());
                updatedQuiz.setOptions(newQuiz.getOptions());
                quizRepository.save(updatedQuiz);
                return new ResponseEntity<>(updatedQuiz, HttpStatus.OK);
            case NOT_FOUND:
                throw new QuizNotFoundException(QUIZ_NOT_FOUND);
            case FORBIDDEN:
                throw new AccessForbiddenException(UNAUTHORIZED_ACCESS);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PatchMapping(value = "/{id}", consumes = {"application/json","application/json-patch+json"})
    public ResponseEntity<Quiz> patchQuiz(@PathVariable("id") long id,
                                          @RequestBody JsonPatch quizPatch)
                                            throws JsonPatchException, JsonProcessingException {
        switch(checkQuizAccessibility(id, true).getStatusCode()) {
            case OK:
                Quiz currentQuiz = quizRepository.getOne(id);
                Quiz patchedQuiz = applyPatchToQuiz(quizPatch, currentQuiz, getObjectMapper());
                patchedQuiz.setUser(getAuthenticatedUser());
                quizRepository.save(patchedQuiz);
                return new ResponseEntity<>(patchedQuiz, HttpStatus.OK);
            case NOT_FOUND:
                throw new QuizNotFoundException(QUIZ_NOT_FOUND);
            case FORBIDDEN:
                throw new AccessForbiddenException(UNAUTHORIZED_ACCESS);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

    }


    private Quiz applyPatchToQuiz(JsonPatch patch,
                                  Quiz targetQuiz,
                                  ObjectMapper objectMapper)
                                    throws JsonPatchException,
                                            JsonProcessingException {

        JsonNode patched = patch.apply(
                        objectMapper.convertValue(
                                targetQuiz, JsonNode.class));
        return objectMapper.treeToValue(patched, Quiz.class);

    }


    public boolean checkUserAuthorities(long quizId) {
        if (quizRepository.findById(quizId).isPresent()) {
            return quizRepository
                    .getOne(quizId)
                    .getUser() ==
                    getAuthenticatedUser();
        } else return false;
    }

    public User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
            return this.userRepository.findByUsername(username);
        } else {
            //username = principal.toString();
            throw new UsernameNotFoundException(NO_USER);
        }
    }

    public Page<Quiz> getQuizzes(Integer pageNo,
                                 Integer pageSize,
                                 String sortByField) {
        return quizService.getAllQuizzesWithPagination(pageNo, pageSize, sortByField);
    }

    public ResponseEntity<String> checkQuizAccessibility(long id,
                                         boolean checkUserCredentials) {
        if (quizRepository.findById(id).isPresent()) {
            if (checkUserCredentials) {
                if (checkUserAuthorities(id)) {
                    return new ResponseEntity<>(HttpStatus.OK);
                } else return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            } else return new ResponseEntity<>(HttpStatus.OK);
        } return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}

class QuizResult {
    private boolean success;
    private String feedback;

    public QuizResult(boolean success) {
        this.success = success;
        if (success) this.feedback = "Congratulations, you're right!";
        else this.feedback = "Wrong answer! Please, try again.";
    }

    public boolean getSuccess() {
        return this.success;
    }

    public String getFeedback() {
        return this.feedback;
    }
}

@Repository
interface QuizRepository extends JpaRepository<Quiz, Long> {


}

@Service
class QuizService {

    @Autowired
    QuizRepository quizRepository;

    public Page<Quiz> getAllQuizzesWithPagination(Integer page,
                                                  Integer pageSize,
                                                  String sortByField) {
        Pageable paging = PageRequest.of(page, pageSize,
                                    Sort.by(sortByField).ascending());

        Page<Quiz> pagedResult = this.quizRepository.findAll(paging);

        return pagedResult;

    }
}

