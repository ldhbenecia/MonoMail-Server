package woozlabs.echo.domain.gmail.dto.draft;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Data
public class GmailDraftGetMessagesCc {
    private String name;
    private String email;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GmailDraftGetMessagesCc that = (GmailDraftGetMessagesCc) o;
        return this.name.equals(that.getName()) &&
                this.email.equals(that.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email);
    }
}
