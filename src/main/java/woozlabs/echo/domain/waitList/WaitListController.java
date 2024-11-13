package woozlabs.echo.domain.waitList;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waitList")
public class WaitListController {

    private final WaitListService waitListService;

    @Autowired
    public WaitListController(WaitListService waitListService) {
        this.waitListService = waitListService;
    }

    @PostMapping
    public ResponseEntity<Void> addWaitList(@RequestBody EmailRequest emailRequest)
            throws ExecutionException, InterruptedException {
        waitListService.addWaitList(emailRequest.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WaitList>> getWaitList() {
        List<WaitList> waitListEmails = waitListService.getWaitList();
        return ResponseEntity.ok(waitListEmails);
    }

    public static class EmailRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
