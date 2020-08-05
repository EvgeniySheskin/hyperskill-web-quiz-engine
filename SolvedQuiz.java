package engine;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class SolvedQuiz {
    @Id
    @Column(name = "solvedQuizId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private long id;

    @Column(name = "solvedBy")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private long userId;

    @Order(value = 1)
    @JsonProperty(value = "id")
    private long quizId;

    @Order(value = 2)
    private LocalDateTime completedAt;

    public String getCompletedAt() {
        return completedAt.toString();
    }

    public long getQuizId() {
        return quizId;
    }

    public long getUserId() {
        return userId;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}

@Repository
interface SolvedQuizRepository extends JpaRepository<SolvedQuiz, Long> {
    public Page<SolvedQuiz> findAllByUserId(long userId, Pageable pageable);
}

@Service
class SolvedQuizService {

    @Autowired
    private SolvedQuizRepository solvedQuizRepository;

    public Page<SolvedQuiz> getAllSolvedQuizzesWithPagination(Integer page,
                                                  Integer pageSize,
                                                  long userId) {
        Pageable paging = PageRequest.of(page, pageSize,
                Sort.by("completedAt").descending());

        Page<SolvedQuiz> pagedResult = this.solvedQuizRepository.findAllByUserId(userId, paging);

        return pagedResult;
    }

}
